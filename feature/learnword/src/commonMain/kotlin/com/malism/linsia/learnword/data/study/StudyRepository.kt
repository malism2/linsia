package com.malism.linsia.learnword.data.study

import co.touchlab.kermit.Logger
import com.malism.linsia.learnword.data.db.ActiveSessionEntity
import com.malism.linsia.learnword.data.db.DailyStudyLogEntity
import com.malism.linsia.learnword.data.db.LearnWordDatabase
import com.malism.linsia.learnword.data.db.UserStudySettingsEntity
import com.malism.linsia.learnword.data.db.WordProgressEntity
import com.malism.linsia.learnword.data.importing.BookImportPipeline
import com.malism.linsia.learnword.domain.DailyTaskBuilder
import com.malism.linsia.learnword.domain.KnowResult
import com.malism.linsia.learnword.domain.LearnWordDefaults
import com.malism.linsia.learnword.domain.SessionCard
import com.malism.linsia.learnword.domain.SessionQueue
import com.malism.linsia.learnword.domain.Sm2Scheduler
import com.malism.linsia.learnword.domain.Sm2State
import com.malism.linsia.learnword.data.importing.StoredSentence
import com.malism.linsia.learnword.domain.StudyCardContent
import com.malism.linsia.learnword.domain.StudyExample
import com.malism.linsia.learnword.domain.formatIpa
import com.malism.linsia.learnword.domain.SummaryStats
import com.malism.linsia.learnword.domain.TaskInput
import com.malism.linsia.learnword.domain.TaskWord
import com.malism.linsia.learnword.domain.WordSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StudyRepository(
    private val db: LearnWordDatabase,
    private val importer: BookImportPipeline,
) {
    private val log = Logger.withTag("Study")
    private val json = Json { ignoreUnknownKeys = true }
    private var queue: SessionQueue? = null

    private val card = MutableStateFlow<StudyCardContent?>(null)
    private val showingBack = MutableStateFlow(false)
    val currentCard: StateFlow<StudyCardContent?> = card
    val isBack: StateFlow<Boolean> = showingBack

    data class HomeState(
        val bookTitle: String,
        val bookId: String,
        val imported: Boolean,
        val bundled: Boolean,
        val canContinue: Boolean,
        val streak: Int,
        val dailyNewWords: Int,
        val loading: Boolean = false,
        val error: String? = null,
        val todayDone: Int = 0,
        val todayTotal: Int = 0,
        val bookSeen: Int = 0,
        val bookTotal: Int = 0,
        val tomorrowReview: Int = 0,
    )

    fun observeActiveBookId(): Flow<String> = db.settingsDao().observe().map { settings ->
        settings?.activeBookId ?: LearnWordDefaults.BundledBookId
    }

    fun observeHome(): Flow<HomeState> = combine(
        db.wordBookDao().observeBooks(),
        db.settingsDao().observe(),
    ) { books, settings ->
        val s = settings ?: UserStudySettingsEntity(
            dailyNewWords = LearnWordDefaults.DefaultNewWords,
            activeBookId = LearnWordDefaults.BundledBookId,
            accentUk = false,
        )
        val book = books.find { it.name == s.activeBookId } ?: books.firstOrNull()
        HomeState(
            bookTitle = book?.title ?: "记单词",
            bookId = book?.name ?: LearnWordDefaults.BundledBookId,
            imported = book?.imported == true,
            bundled = book?.bundled == true,
            canContinue = false,
            streak = 0,
            dailyNewWords = s.dailyNewWords,
        )
    }

    suspend fun homeSnapshot(): HomeState {
        val settings = settingsOrDefault()
        val book = db.wordBookDao().getBook(settings.activeBookId)
        val session = db.sessionDao().get()
        val today = today()
        val canContinue = session != null &&
            session.date == today.toString() &&
            !session.completed &&
            session.bookId == settings.activeBookId
        val (todayDone, todayTotal) = todayProgress(session, settings, today)
        val bookId = settings.activeBookId
        val progress = db.progressDao().getByBook(bookId)
        val todayEpoch = today.toEpochDays().toInt()
        return HomeState(
            bookTitle = book?.title ?: "记单词",
            bookId = bookId,
            imported = book?.imported == true,
            bundled = book?.bundled == true,
            canContinue = canContinue,
            streak = computeStreak(bookId, today),
            dailyNewWords = settings.dailyNewWords,
            todayDone = todayDone,
            todayTotal = todayTotal,
            bookSeen = progress.count { it.seen },
            bookTotal = db.wordDao().countByBook(bookId),
            tomorrowReview = progress.count { it.seen && it.nextReviewEpochDay == todayEpoch + 1 },
        )
    }

    suspend fun ensureActiveBookImported(): Result<Unit> {
        val settings = settingsOrDefault()
        val book = db.wordBookDao().getBook(settings.activeBookId)
            ?: return Result.failure(IllegalStateException("未找到词书"))
        return importer.importIfNeeded(book.name, book.url, book.bundled).map { }
    }

    suspend fun importBook(bookId: String): Result<Int> {
        val book = db.wordBookDao().getBook(bookId) ?: return Result.failure(IllegalStateException("未找到词书"))
        return importer.importIfNeeded(book.name, book.url, book.bundled)
    }

    suspend fun startOrContinue(): Result<StudyCardContent?> = runCatching {
        val settings = settingsOrDefault()
        val today = today()
        val existing = db.sessionDao().get()
        if (existing != null && existing.date == today.toString() &&
            existing.bookId == settings.activeBookId && !existing.completed
        ) {
            restore(existing)
        } else {
            val leftovers = leftoversFrom(existing, today, settings.activeBookId)
            buildNewSession(settings, today, leftovers)
        }
        showingBack.value = false
        refreshCard(settings)
        card.value
    }

    fun showBack() {
        showingBack.value = true
    }

    suspend fun know(): StudyOutcome {
        val q = queue ?: return StudyOutcome.Idle
        val settings = settingsOrDefault()
        val current = q.current ?: return StudyOutcome.Idle
        val result: KnowResult = q.know()
        bumpEncounters(firstPassKnown = current.firstPassKnown ?: true)
        result.graduated?.let { graduate(it, settings) }
        persistSession(completed = q.isEmpty)
        showingBack.value = false
        if (q.isEmpty) {
            return finish(settings)
        }
        refreshCard(settings)
        return StudyOutcome.Next
    }

    suspend fun rememberedAfterUnknown(): StudyOutcome {
        val q = queue ?: return StudyOutcome.Idle
        val current = q.current ?: return StudyOutcome.Idle
        bumpEncounters(firstPassKnown = false)
        q.markUnknownAndReinsert()
        persistSession(completed = false)
        showingBack.value = false
        refreshCard(settingsOrDefault())
        return StudyOutcome.Next
    }

    suspend fun addElapsed(seconds: Long) {
        val session = db.sessionDao().get() ?: return
        db.sessionDao().upsert(session.copy(elapsedSec = session.elapsedSec + seconds))
    }

    suspend fun updateSettings(dailyNew: Int? = null, accentUk: Boolean? = null, activeBookId: String? = null) {
        val current = settingsOrDefault()
        db.settingsDao().upsert(
            current.copy(
                dailyNewWords = (dailyNew ?: current.dailyNewWords)
                    .coerceIn(LearnWordDefaults.MinNewWords, LearnWordDefaults.MaxNewWords),
                accentUk = accentUk ?: current.accentUk,
                activeBookId = activeBookId ?: current.activeBookId,
            ),
        )
    }

    suspend fun settingsOrDefault(): UserStudySettingsEntity =
        db.settingsDao().get() ?: UserStudySettingsEntity(
            dailyNewWords = LearnWordDefaults.DefaultNewWords,
            activeBookId = LearnWordDefaults.BundledBookId,
            accentUk = false,
        )

    private suspend fun buildNewSession(
        settings: UserStudySettingsEntity,
        today: LocalDate,
        leftovers: List<TaskWord>,
    ) {
        val bookId = settings.activeBookId
        val words = db.wordDao().getByBook(bookId)
        val progress = db.progressDao().getByBook(bookId).associateBy { it.wordId }
        val todayEpoch = today.toEpochDays().toInt()
        val newWords = words.filter { progress[it.id]?.seen != true }
            .map { TaskWord(it.id, it.ordinal, WordSource.New) }
        val due = words.mapNotNull { w ->
            val p = progress[w.id] ?: return@mapNotNull null
            val dueDay = p.nextReviewEpochDay ?: return@mapNotNull null
            if (p.seen && dueDay <= todayEpoch) TaskWord(w.id, w.ordinal, WordSource.Review) else null
        }
        val pack = DailyTaskBuilder.build(
            TaskInput(
                newWords = newWords,
                dueReview = due,
                leftovers = leftovers,
                dailyNewLimit = settings.dailyNewWords,
            ),
        )
        val cards = pack.map {
            SessionCard(it.wordId, LearnWordDefaults.InitialLife, it.source)
        }
        queue = SessionQueue(cards)
        db.sessionDao().upsert(
            ActiveSessionEntity(
                date = today.toString(),
                bookId = bookId,
                queueJson = json.encodeToString(cards.map { it.toPersisted() }),
                graduatedJson = json.encodeToString(emptyList<Long>()),
                firstPassCorrect = 0,
                totalEncounters = 0,
                elapsedSec = 0,
                completed = cards.isEmpty(),
                leftoverJson = json.encodeToString(emptyList<Long>()),
            ),
        )
        if (cards.isEmpty()) {
            log.i { "Empty pack for $bookId" }
        }
    }

    private suspend fun leftoversFrom(
        existing: ActiveSessionEntity?,
        today: LocalDate,
        bookId: String,
    ): List<TaskWord> {
        if (existing == null || existing.completed || existing.bookId != bookId) return emptyList()
        val sessionDate = runCatching { LocalDate.parse(existing.date) }.getOrNull() ?: return emptyList()
        if (sessionDate >= today) return emptyList()
        val leftoverIds = json.decodeFromString<List<PersistedCard>>(existing.queueJson).map { it.wordId }
            .ifEmpty { json.decodeFromString<List<Long>>(existing.leftoverJson.ifBlank { "[]" }) }
        return leftoverIds.map { TaskWord(it, 0, WordSource.Leftover) }
    }

    private suspend fun restore(entity: ActiveSessionEntity) {
        val cards = json.decodeFromString<List<PersistedCard>>(entity.queueJson).map { it.toDomain() }
        queue = SessionQueue(cards)
    }

    private suspend fun persistSession(completed: Boolean) {
        val current = db.sessionDao().get() ?: return
        val q = queue ?: return
        val leftover = q.remaining.map { it.wordId }
        db.sessionDao().upsert(
            current.copy(
                queueJson = json.encodeToString(q.remaining.map { it.toPersisted() }),
                leftoverJson = json.encodeToString(leftover),
                completed = completed,
            ),
        )
    }

    private suspend fun bumpEncounters(firstPassKnown: Boolean) {
        val current = db.sessionDao().get() ?: return
        db.sessionDao().upsert(
            current.copy(
                totalEncounters = current.totalEncounters + 1,
                firstPassCorrect = current.firstPassCorrect + if (firstPassKnown) 1 else 0,
            ),
        )
    }

    private suspend fun graduate(card: SessionCard, settings: UserStudySettingsEntity) {
        val today = today()
        val todayEpoch = today.toEpochDays().toInt()
        val existing = db.progressDao().get(settings.activeBookId, card.wordId)
        val next = Sm2Scheduler.applySuccess(
            Sm2State(
                easeFactor = existing?.easeFactor ?: Sm2Scheduler.DefaultEase,
                intervalDays = existing?.intervalDays ?: 0,
                repetitions = existing?.repetitions ?: 0,
            ),
        )
        val nextDay = Sm2Scheduler.nextReviewDate(today, next.intervalDays)
        db.progressDao().upsert(
            WordProgressEntity(
                bookId = settings.activeBookId,
                wordId = card.wordId,
                easeFactor = next.easeFactor,
                intervalDays = next.intervalDays,
                repetitions = next.repetitions,
                nextReviewEpochDay = nextDay.toEpochDays().toInt(),
                lastReviewEpochDay = todayEpoch,
                seen = true,
            ),
        )
        val session = db.sessionDao().get() ?: return
        val graduated = json.decodeFromString<List<Long>>(session.graduatedJson.ifBlank { "[]" }) + card.wordId
        db.sessionDao().upsert(session.copy(graduatedJson = json.encodeToString(graduated)))
        val logRow = db.studyLogDao().get(today.toString(), settings.activeBookId)
            ?: DailyStudyLogEntity(
                date = today.toString(),
                bookId = settings.activeBookId,
                newGraduated = 0,
                reviewGraduated = 0,
                durationSec = 0,
                firstPassCorrect = 0,
                totalEncounters = 0,
                taskCompleted = false,
            )
        val updated = when (card.source) {
            WordSource.New -> logRow.copy(newGraduated = logRow.newGraduated + 1)
            WordSource.Review, WordSource.Leftover -> logRow.copy(reviewGraduated = logRow.reviewGraduated + 1)
        }
        db.studyLogDao().upsert(updated)
        log.i { "Graduated wordId=${card.wordId}" }
    }

    private suspend fun finish(settings: UserStudySettingsEntity): StudyOutcome.Finished {
        val session = db.sessionDao().get()
        val today = today()
        val elapsed = session?.elapsedSec ?: 0L
        val firstPass = session?.firstPassCorrect ?: 0
        val encounters = session?.totalEncounters ?: 0
        val logRow = db.studyLogDao().get(today.toString(), settings.activeBookId)
        db.studyLogDao().upsert(
            (logRow ?: DailyStudyLogEntity(
                date = today.toString(),
                bookId = settings.activeBookId,
                newGraduated = 0,
                reviewGraduated = 0,
                durationSec = elapsed,
                firstPassCorrect = firstPass,
                totalEncounters = encounters,
                taskCompleted = true,
            )).copy(
                durationSec = elapsed,
                firstPassCorrect = firstPass,
                totalEncounters = encounters,
                taskCompleted = true,
            ),
        )
        persistSession(completed = true)
        card.value = null
        val streak = computeStreak(settings.activeBookId, today)
        return StudyOutcome.Finished(
            SummaryStats(
                graduated = (logRow?.newGraduated ?: 0) + (logRow?.reviewGraduated ?: 0),
                elapsedSec = elapsed,
                firstPassCorrect = firstPass,
                totalEncounters = encounters,
                streak = streak,
            ),
        )
    }

    private suspend fun refreshCard(settings: UserStudySettingsEntity) {
        val current = queue?.current
        if (current == null) {
            card.value = null
            return
        }
        val word = db.wordDao().getById(current.wordId) ?: run {
            card.value = null
            return
        }
        val trans = runCatching { json.decodeFromString<List<String>>(word.transJson) }.getOrDefault(emptyList())
        val phone = if (settings.accentUk) word.ukPhone else word.usPhone
        val (index, total) = packProgress()
        card.value = StudyCardContent(
            wordId = word.id,
            headWord = word.headWord,
            phonetic = formatIpa(phone),
            transLines = trans,
            examples = parseExamples(word.sentenceJson),
            source = current.source,
            life = current.life,
            index = index,
            total = total,
        )
    }

    private suspend fun packProgress(): Pair<Int, Int> {
        val graduatedIds = db.sessionDao().get()
            ?.graduatedJson
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { json.decodeFromString<List<Long>>(it) }.getOrDefault(emptyList()) }
            ?: emptyList()
        val remainingIds = queue?.remaining.orEmpty().map { it.wordId }
        val total = (graduatedIds + remainingIds).toSet().size
        if (total == 0) return 0 to 0
        val index = (graduatedIds.toSet().size + 1).coerceAtMost(total)
        return index to total
    }

    private fun parseExamples(raw: String?): List<StudyExample> {
        if (raw.isNullOrBlank()) return emptyList()
        runCatching { json.decodeFromString<List<StoredSentence>>(raw) }.getOrNull()
            ?.map { StudyExample(it.en, it.cn) }
            ?.filter { it.english.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }
        val parts = raw.split('\n', limit = 2)
        val en = parts[0].trim()
        if (en.isEmpty()) return emptyList()
        return listOf(StudyExample(en, parts.getOrNull(1)?.trim().orEmpty()))
    }

    private suspend fun todayProgress(
        session: ActiveSessionEntity?,
        settings: UserStudySettingsEntity,
        today: LocalDate,
    ): Pair<Int, Int> {
        if (session != null && session.date == today.toString() && session.bookId == settings.activeBookId) {
            val graduatedIds = session.graduatedJson.takeIf { it.isNotBlank() }
                ?.let { runCatching { json.decodeFromString<List<Long>>(it) }.getOrDefault(emptyList()) }
                ?: emptyList()
            val remainingIds = runCatching {
                json.decodeFromString<List<PersistedCard>>(session.queueJson).map { it.wordId }
            }.getOrDefault(emptyList())
            val total = (graduatedIds + remainingIds).toSet().size
            return graduatedIds.toSet().size to total
        }
        if (!db.wordBookDao().getBook(settings.activeBookId)?.imported.let { it == true }) {
            return 0 to 0
        }
        val pack = previewTodayPack(settings, today, session)
        return 0 to pack.size
    }

    private suspend fun previewTodayPack(
        settings: UserStudySettingsEntity,
        today: LocalDate,
        existing: ActiveSessionEntity?,
    ): List<TaskWord> {
        val leftovers = leftoversFrom(existing, today, settings.activeBookId)
        val bookId = settings.activeBookId
        val words = db.wordDao().getByBook(bookId)
        val progress = db.progressDao().getByBook(bookId).associateBy { it.wordId }
        val todayEpoch = today.toEpochDays().toInt()
        val newWords = words.filter { progress[it.id]?.seen != true }
            .map { TaskWord(it.id, it.ordinal, WordSource.New) }
        val due = words.mapNotNull { w ->
            val p = progress[w.id] ?: return@mapNotNull null
            val dueDay = p.nextReviewEpochDay ?: return@mapNotNull null
            if (p.seen && dueDay <= todayEpoch) TaskWord(w.id, w.ordinal, WordSource.Review) else null
        }
        return DailyTaskBuilder.build(
            TaskInput(
                newWords = newWords,
                dueReview = due,
                leftovers = leftovers,
                dailyNewLimit = settings.dailyNewWords,
            ),
        )
    }

    private suspend fun computeStreak(bookId: String, today: LocalDate): Int {
        val logs = db.studyLogDao().recent(bookId).filter { it.taskCompleted }.map { it.date }.toSet()
        var cursor = today
        if (today.toString() !in logs) {
            cursor = today.minus(DatePeriod(days = 1))
        }
        var count = 0
        while (cursor.toString() in logs) {
            count++
            cursor = cursor.minus(DatePeriod(days = 1))
        }
        return count
    }

    private fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}

sealed interface StudyOutcome {
    data object Idle : StudyOutcome
    data object Next : StudyOutcome
    data class Finished(val stats: SummaryStats) : StudyOutcome
}

@Serializable
private data class PersistedCard(
    val wordId: Long,
    val life: Int,
    val source: WordSource,
    val firstPassKnown: Boolean? = null,
)

private fun SessionCard.toPersisted() = PersistedCard(wordId, life, source, firstPassKnown)
private fun PersistedCard.toDomain() = SessionCard(wordId, life, source, firstPassKnown)
