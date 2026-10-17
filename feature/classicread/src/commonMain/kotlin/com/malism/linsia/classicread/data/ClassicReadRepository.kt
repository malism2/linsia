package com.malism.linsia.classicread.data

import co.touchlab.kermit.Logger
import com.malism.linsia.classicread.data.db.BookEntity
import com.malism.linsia.classicread.data.db.CatalogStateEntity
import com.malism.linsia.classicread.data.db.ChapterEntity
import com.malism.linsia.classicread.data.db.ClassicReadDao
import com.malism.linsia.classicread.data.db.ProgressEntity
import com.malism.linsia.classicread.data.epub.EpubParser
import com.malism.linsia.classicread.data.epub.classicEpubPath
import com.malism.linsia.classicread.domain.ClassicBook
import com.malism.linsia.classicread.domain.ClassicChapter
import com.malism.linsia.classicread.domain.ClassicChapterMeta
import com.malism.linsia.classicread.domain.ClassicReadDefaults
import com.malism.linsia.classicread.domain.SeBooksCatalogFile
import com.malism.linsia.classicread.domain.ensureSeEpubDownloadUrl
import com.malism.linsia.classicread.generated.Res
import com.malism.linsia.core.io.deleteIfExists
import com.malism.linsia.core.io.requireValidZipFile
import com.malism.linsia.core.net.downloadToFile
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

class ClassicReadRepository(
    private val http: HttpClient,
    private val dao: ClassicReadDao,
) {
    private val log = Logger.withTag("ClassicRead")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun observeCatalog(): Flow<List<ClassicBook>> = combine(
        dao.observeListedBooks(),
        dao.observeProgress(),
        dao.observeChapterCounts(),
    ) { books, progress, counts ->
        val progressById = progress.associateBy { it.bookId }
        val countById = counts.associate { it.bookId to it.count }
        books.map { book ->
            book.toDomain(
                chapterCount = countById[book.id] ?: 0,
                progressChapterIndex = progressById[book.id]?.chapterIndex,
            )
        }
    }

    fun observeBook(id: String): Flow<ClassicBook?> = combine(
        dao.observeBook(id),
        dao.observeProgress(id),
        dao.observeChapterCounts(),
    ) { book, progress, counts ->
        book?.toDomain(
            chapterCount = counts.firstOrNull { it.bookId == id }?.count ?: 0,
            progressChapterIndex = progress?.chapterIndex,
        )
    }

    fun observeChapterTitles(bookId: String): Flow<List<ClassicChapterMeta>> =
        dao.observeChapterTitles(bookId).map { rows ->
            rows.map { ClassicChapterMeta(it.chapterIndex, it.title) }
        }

    fun observeChapter(bookId: String, chapterIndex: Int): Flow<ClassicChapter?> =
        dao.observeChapter(bookId, chapterIndex).map { row ->
            row?.let {
                ClassicChapter(it.bookId, it.chapterIndex, it.title, it.body)
            }
        }

    fun observeProgress(bookId: String): Flow<ProgressEntity?> = dao.observeProgress(bookId)

    suspend fun getChapterBodies(bookId: String): List<String> = dao.getChapterBodies(bookId)

    suspend fun ensureSeeded() {
        if (dao.getCatalogState() != null && dao.getBooks().isNotEmpty()) return
        val catalog = readBundledCatalog()
        applyCatalog(catalog)
        log.i { "Seeded catalog version=${catalog.version} books=${catalog.books.size}" }
    }

    suspend fun checkRemoteUpdate(): Result<Boolean> = runCatching {
        val remoteText = http.get(ClassicReadDefaults.RemoteCatalogUrl) {
            header(HttpHeaders.UserAgent, ClassicReadDefaults.UserAgent)
        }.bodyAsText()
        val remote = json.decodeFromString<SeBooksCatalogFile>(remoteText)
        val localVersion = dao.getCatalogState()?.version ?: 0
        if (remote.version > localVersion) {
            applyCatalog(remote)
            log.i { "Catalog updated $localVersion -> ${remote.version}" }
            true
        } else {
            false
        }
    }.onFailure { log.w { "Catalog update failed: ${it.message}" } }

    suspend fun ensureDownloaded(
        bookId: String,
        onProgress: (received: Long, total: Long?) -> Unit = { _, _ -> },
    ) {
        val existing = dao.getBook(bookId) ?: error(ClassicReadDefaults.FetchFailedMessage)
        if (existing.downloaded && dao.chapterCount(bookId) > 0) return
        if (existing.epubUrl.isBlank()) error(ClassicReadDefaults.UnavailableMessage)
        val epubPath = classicEpubPath(bookId)
        deleteIfExists(epubPath)
        val epubUrl = existing.epubUrl.ensureSeEpubDownloadUrl()
        http.downloadToFile(epubUrl, epubPath, onProgress) {
            header(HttpHeaders.UserAgent, ClassicReadDefaults.UserAgent)
            header(HttpHeaders.Accept, "application/epub+zip, application/octet-stream, */*")
        }
        requireValidZipFile(epubPath, ClassicReadDefaults.FetchFailedMessage)
        val parsed = EpubParser.parse(epubPath)
        if (parsed.isEmpty()) error(ClassicReadDefaults.FetchFailedMessage)
        dao.deleteChapters(bookId)
        dao.insertChapters(
            parsed.mapIndexed { index, chapter ->
                ChapterEntity(
                    bookId = bookId,
                    chapterIndex = index,
                    title = chapter.title,
                    body = chapter.body,
                )
            },
        )
        dao.setDownloaded(bookId, true, Clock.System.now().toEpochMilliseconds())
        deleteIfExists(epubPath)
    }

    suspend fun saveProgress(bookId: String, chapterIndex: Int) {
        dao.upsertProgress(
            ProgressEntity(
                bookId = bookId,
                chapterIndex = chapterIndex,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    private suspend fun applyCatalog(catalog: SeBooksCatalogFile) {
        val existing = dao.getBooks().associateBy { it.id }
        val books = catalog.books.map { dto ->
            val prev = existing[dto.id]
            BookEntity(
                id = dto.id,
                title = dto.title,
                author = dto.author,
                rank = dto.rank,
                coverUrl = dto.coverUrl,
                epubUrl = dto.epubUrl,
                listed = true,
                downloaded = prev?.downloaded == true,
                downloadedAt = prev?.downloadedAt,
            )
        }
        dao.upsertBooks(books)
        val ids = catalog.books.map { it.id }
        if (ids.isNotEmpty()) dao.unlistMissing(ids)
        dao.upsertCatalogState(
            CatalogStateEntity(
                version = catalog.version,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun readBundledCatalog(): SeBooksCatalogFile {
        val bytes = Res.readBytes("files/se_books.json")
        return json.decodeFromString(bytes.decodeToString())
    }
}

private fun BookEntity.toDomain(chapterCount: Int, progressChapterIndex: Int?) = ClassicBook(
    id = id,
    rank = rank,
    title = title,
    author = author,
    coverUrl = coverUrl,
    epubUrl = epubUrl,
    downloaded = downloaded,
    chapterCount = chapterCount,
    progressChapterIndex = progressChapterIndex,
)
