package com.malism.linsia.learnword.data.catalog

import co.touchlab.kermit.Logger
import com.malism.linsia.learnword.data.db.CatalogStateEntity
import com.malism.linsia.learnword.data.db.LearnWordDatabase
import com.malism.linsia.learnword.data.db.UserStudySettingsEntity
import com.malism.linsia.learnword.data.db.WordBookMetaEntity
import com.malism.linsia.learnword.domain.LearnWordDefaults
import com.malism.linsia.learnword.generated.Res
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

class CatalogRepository(
    private val db: LearnWordDatabase,
    private val http: HttpClient,
) {
    private val log = Logger.withTag("Catalog")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun observeBooks(): Flow<List<WordBookMetaEntity>> = db.wordBookDao().observeBooks()

    suspend fun ensureSeeded() {
        if (db.wordBookDao().getCatalogState() != null && db.wordBookDao().getBooks().isNotEmpty()) {
            ensureSettings()
            return
        }
        val catalog = readBundledCatalog()
        applyCatalog(catalog, keepImportState = false)
        ensureSettings()
        log.i { "Seeded catalog version=${catalog.version} books=${catalog.reops.size}" }
    }

    suspend fun checkRemoteUpdate(): Result<Boolean> = runCatching {
        val remoteText = http.get(LearnWordDefaults.RemoteCatalogUrl).bodyAsText()
        val remote = json.decodeFromString<WordCatalogFile>(remoteText)
        val localVersion = db.wordBookDao().getCatalogState()?.version ?: 0
        if (remote.version > localVersion) {
            applyCatalog(remote, keepImportState = true)
            log.i { "Catalog updated $localVersion -> ${remote.version}" }
            true
        } else {
            false
        }
    }.onFailure { log.w { "Catalog update failed: ${it.message}" } }

    private suspend fun applyCatalog(catalog: WordCatalogFile, keepImportState: Boolean) {
        val existing = db.wordBookDao().getBooks().associateBy { it.name }
        val books = catalog.reops.map { dto ->
            val prev = existing[dto.name]
            WordBookMetaEntity(
                name = dto.name,
                title = dto.title,
                url = dto.downloadUrl,
                bundled = dto.name == LearnWordDefaults.BundledBookId,
                imported = if (keepImportState) prev?.imported == true else false,
                importedAt = if (keepImportState) prev?.importedAt else null,
            )
        }
        db.wordBookDao().upsert(books)
        db.wordBookDao().upsertCatalogState(
            CatalogStateEntity(
                version = catalog.version,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    private suspend fun ensureSettings() {
        if (db.settingsDao().get() == null) {
            db.settingsDao().upsert(
                UserStudySettingsEntity(
                    dailyNewWords = LearnWordDefaults.DefaultNewWords,
                    activeBookId = LearnWordDefaults.BundledBookId,
                    accentUk = false,
                ),
            )
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun readBundledCatalog(): WordCatalogFile {
        val bytes = Res.readBytes("files/learn_dist.json")
        return json.decodeFromString(bytes.decodeToString())
    }
}
