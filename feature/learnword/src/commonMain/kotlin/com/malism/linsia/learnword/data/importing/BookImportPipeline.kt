package com.malism.linsia.learnword.data.importing

import co.touchlab.kermit.Logger
import com.malism.linsia.learnword.data.db.LearnWordDatabase
import com.malism.linsia.learnword.data.db.WordEntity
import com.malism.linsia.learnword.data.zip.ZipArchive
import com.malism.linsia.learnword.domain.LearnWordDefaults
import com.malism.linsia.learnword.generated.Res
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import org.jetbrains.compose.resources.ExperimentalResourceApi

class BookImportPipeline(
    private val db: LearnWordDatabase,
    private val http: HttpClient,
) {
    private val log = Logger.withTag("BookImport")

    suspend fun importIfNeeded(bookId: String, url: String, bundled: Boolean): Result<Int> =
        runCatching {
            val existing = db.wordDao().countByBook(bookId)
            if (existing > 0) {
                markImported(bookId)
                return@runCatching existing
            }
            val zipBytes = withContext(Dispatchers.IO) {
                if (bundled && bookId == LearnWordDefaults.BundledBookId) {
                    readBundledZip()
                } else {
                    http.get(url).bodyAsBytes()
                }
            }
            val count = withContext(Dispatchers.Default) { parseAndInsert(bookId, zipBytes) }
            markImported(bookId)
            log.i { "Imported $bookId words=$count" }
            count
        }.onFailure { log.e { "Import failed $bookId: ${it.message}" } }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun readBundledZip(): ByteArray =
        Res.readBytes("files/CET4luan_1.zip")

    private suspend fun parseAndInsert(bookId: String, zipBytes: ByteArray): Int {
        val files = ZipArchive.extract(zipBytes)
        val jsonText = files.entries
            .firstOrNull { it.key.endsWith(".json", ignoreCase = true) }
            ?.value
            ?.decodeToString()
            ?: error("No json in zip")
        val parsed = YoudaoParser.parseConcatenated(jsonText)
        val entities = parsed.map {
            WordEntity(
                bookId = bookId,
                headWord = it.headWord,
                ukPhone = it.ukPhone,
                usPhone = it.usPhone,
                transJson = it.transJson,
                sentenceJson = it.sentenceJson,
                extraJson = null,
                ordinal = it.ordinal,
            )
        }
        entities.chunked(400).forEach { chunk ->
            db.wordDao().insertAll(chunk)
        }
        return entities.size
    }

    private suspend fun markImported(bookId: String) {
        val meta = db.wordBookDao().getBook(bookId) ?: return
        db.wordBookDao().update(
            meta.copy(imported = true, importedAt = Clock.System.now().toEpochMilliseconds()),
        )
    }
}
