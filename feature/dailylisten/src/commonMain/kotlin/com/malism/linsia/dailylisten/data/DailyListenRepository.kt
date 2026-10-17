package com.malism.linsia.dailylisten.data

import com.malism.linsia.core.net.downloadToFile
import com.malism.linsia.dailylisten.data.db.DailyListenDao
import com.malism.linsia.dailylisten.data.db.LessonEntity
import com.malism.linsia.dailylisten.data.db.ProgressEntity
import com.malism.linsia.dailylisten.data.fs.dailyListenAudioPath
import com.malism.linsia.dailylisten.data.parse.epochToDateIso
import com.malism.linsia.dailylisten.data.parse.parseArticleHtml
import com.malism.linsia.dailylisten.data.parse.parseListingHtml
import com.malism.linsia.dailylisten.domain.DailyListenDefaults
import com.malism.linsia.dailylisten.domain.HistoryItem
import com.malism.linsia.dailylisten.domain.ListenLesson
import com.malism.linsia.dailylisten.domain.ListenProgress
import com.malism.linsia.dailylisten.domain.ListenStreak
import com.malism.linsia.dailylisten.domain.ListingCandidate
import com.malism.linsia.dailylisten.domain.filterCandidatesNotAfterToday
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlin.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.FileSystem
import okio.Path.Companion.toPath

@OptIn(ExperimentalCoroutinesApi::class)
class DailyListenRepository(
    private val client: HttpClient,
    private val dao: DailyListenDao,
) {
    private val _todayLessonId = MutableStateFlow<String?>(null)
    val todayLessonId: StateFlow<String?> = _todayLessonId

    fun observeTodayLesson(): Flow<ListenLesson?> =
        todayLessonId.flatMapLatest { id ->
            if (id == null) flowOf(null)
            else dao.observeLesson(id).map { it?.toDomain() }
        }

    fun observeLesson(id: String): Flow<ListenLesson?> =
        dao.observeLesson(id).map { it?.toDomain() }

    fun observeProgress(lessonId: String): Flow<ListenProgress?> =
        dao.observeProgress(lessonId).map { it?.toDomain() }

    fun observeHistory(): Flow<List<HistoryItem>> =
        combine(dao.observeHistoryLessons(), dao.observeAllProgress()) { lessons, progress ->
            val byId = progress.associateBy { it.lessonId }
            lessons.map { lesson ->
                HistoryItem(lesson = lesson.toDomain(), progress = byId[lesson.id]?.toDomain())
            }
        }

    suspend fun streakDays(): Int {
        val dates = dao.completedDates().filterNotNull().toSet()
        return ListenStreak.compute(dates, today())
    }

    suspend fun refreshAndResolveToday(): ListenLesson {
        val listingHtml = fetchText(listingUrl())
        val candidates = parseListingHtml(listingHtml)
        if (candidates.isEmpty()) error("没有获取到听力列表，请检查网络后重试")
        val todayIso = today().toString()
        val filtered = filterCandidatesNotAfterToday(candidates, todayIso, ::epochToDateIso)
            .take(DailyListenDefaults.CandidateLimit)
        if (filtered.isEmpty()) error("暂无可用听力内容")

        for (candidate in filtered) {
            val lesson = ensureLessonWithAudio(candidate) ?: continue
            ensureProgressRow(lesson.id)
            _todayLessonId.value = lesson.id
            return lesson
        }
        error("暂未找到带音频的听力内容")
    }

    suspend fun saveProgress(lessonId: String, positionMs: Long) {
        val existing = dao.getProgress(lessonId)
        dao.upsertProgress(
            ProgressEntity(
                lessonId = lessonId,
                positionMs = positionMs.coerceAtLeast(0L),
                completed = existing?.completed == true,
                completedDate = existing?.completedDate,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    suspend fun markCompleted(lessonId: String) {
        val existing = dao.getProgress(lessonId)
        val date = today().toString()
        dao.upsertProgress(
            ProgressEntity(
                lessonId = lessonId,
                positionMs = existing?.positionMs ?: 0L,
                completed = true,
                completedDate = existing?.completedDate ?: date,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    suspend fun maybeAutoComplete(lessonId: String, positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        val ratio = positionMs.toFloat() / durationMs.toFloat()
        if (ratio >= DailyListenDefaults.CompleteThreshold) {
            markCompleted(lessonId)
        } else {
            saveProgress(lessonId, positionMs)
        }
    }

    /**
     * Downloads the remote MP3 with a whitelisted Referer (CDN hotlink protection),
     * caches locally, and returns an absolute filesystem path for playback.
     */
    suspend fun ensureLocalAudio(lessonId: String, remoteUrl: String): String {
        val path = dailyListenAudioPath(lessonId)
        val dest = path.toPath()
        if (FileSystem.SYSTEM.exists(dest)) {
            val size = FileSystem.SYSTEM.metadata(dest).size
            if (size != null && size > 0L) return path
        }
        client.downloadToFile(remoteUrl, path) {
            header(HttpHeaders.UserAgent, DailyListenDefaults.UserAgent)
            header(HttpHeaders.Referrer, "https://${DailyListenDefaults.SourceHost}/")
        }
        return path
    }

    private suspend fun ensureProgressRow(lessonId: String) {
        if (dao.getProgress(lessonId) != null) return
        dao.upsertProgress(
            ProgressEntity(
                lessonId = lessonId,
                positionMs = 0L,
                completed = false,
                completedDate = null,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    private suspend fun ensureLessonWithAudio(candidate: ListingCandidate): ListenLesson? {
        val cached = dao.getLesson(candidate.id)
        if (cached != null && cached.audioUrl.isNotBlank()) {
            return cached.toDomain()
        }
        val html = fetchText(candidate.sourceUrl)
        val parsed = parseArticleHtml(html)
        val audio = parsed.audioUrl?.takeIf { it.isNotBlank() } ?: return null
        val dateIso = epochToDateIso(candidate.publishedEpoch)
        val entity = LessonEntity(
            id = candidate.id,
            title = candidate.title,
            publishedAt = candidate.publishedAt,
            publishedDate = dateIso,
            publishedEpoch = candidate.publishedEpoch,
            sourceUrl = candidate.sourceUrl,
            transcript = parsed.body,
            audioUrl = audio,
            cachedAt = Clock.System.now().toEpochMilliseconds(),
        )
        dao.upsertLesson(entity)
        return entity.toDomain()
    }

    private suspend fun fetchText(url: String): String {
        val response = client.get(url) {
            header(HttpHeaders.UserAgent, DailyListenDefaults.UserAgent)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        }
        if (!response.status.isSuccess()) {
            error("请求失败 HTTP ${response.status.value}")
        }
        return response.bodyAsText()
    }

    private fun listingUrl(): String =
        "https://${DailyListenDefaults.SourceHost}/${DailyListenDefaults.ListingPath}"

    private fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}

private fun LessonEntity.toDomain() = ListenLesson(
    id = id,
    title = title,
    publishedAt = publishedAt,
    publishedDate = publishedDate,
    publishedEpoch = publishedEpoch,
    sourceUrl = sourceUrl,
    transcript = transcript,
    audioUrl = audioUrl,
)

private fun ProgressEntity.toDomain() = ListenProgress(
    lessonId = lessonId,
    positionMs = positionMs,
    completed = completed,
    completedDate = completedDate,
)
