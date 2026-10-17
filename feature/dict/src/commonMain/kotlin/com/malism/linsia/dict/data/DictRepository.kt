package com.malism.linsia.dict.data

import co.touchlab.kermit.Logger
import com.malism.linsia.dict.data.db.DictDatabase
import com.malism.linsia.dict.data.fs.deleteIfExists
import com.malism.linsia.dict.data.fs.dictDatabaseExists
import com.malism.linsia.dict.data.fs.dictDatabasePath
import com.malism.linsia.dict.data.fs.dictDbStagingPath
import com.malism.linsia.dict.data.fs.dictZipTempPath
import com.malism.linsia.dict.data.fs.readFilePrefix
import com.malism.linsia.dict.data.fs.replaceFile
import com.malism.linsia.core.net.downloadToFile
import com.malism.linsia.dict.data.zip.extractZipEntryToFile
import com.malism.linsia.dict.domain.DictCatalogFile
import com.malism.linsia.dict.domain.DictDefaults
import com.malism.linsia.dict.domain.DictEntry
import com.malism.linsia.dict.domain.normalizeQuery
import com.malism.linsia.dict.generated.Res
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

class DictRepository(
    private val http: HttpClient,
    private val settings: Settings,
    private val database: DictDatabase,
) {
    private val log = Logger.withTag("Dict")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun isReady(): Boolean =
        settings.getBoolean(DictDefaults.SettingsReadyKey, false) && dictDatabaseExists()

    fun localVersion(): Int = settings.getInt(DictDefaults.SettingsVersionKey, 0)

    suspend fun ensureReady(
        onProgress: (received: Long, total: Long?) -> Unit = { _, _ -> },
        forceUpdate: Boolean = false,
    ): Result<Unit> = runCatching {
        if (isReady() && !forceUpdate) {
            database.openIfNeeded()
            val remote = fetchRemoteCatalog().getOrNull()
            if (remote != null && remote.version > localVersion()) {
                log.i { "Dict update available ${localVersion()} -> ${remote.version}" }
                downloadAndInstall(remote, onProgress)
            }
            return@runCatching
        }
        val catalog = resolveCatalog()
        downloadAndInstall(catalog, onProgress)
    }.onFailure { log.w { "ensureReady failed: ${it.message}" } }

    suspend fun search(query: String): List<DictEntry> {
        val q = normalizeQuery(query)
        if (q.isEmpty() || !isReady()) return emptyList()
        database.openIfNeeded()
        val exact = database.findExact(q)
        val prefix = database.searchPrefix(q)
        if (exact == null) return prefix
        return listOf(exact) + prefix.filter { it.wordLower != exact.wordLower }
    }

    suspend fun getEntry(wordLower: String): DictEntry? {
        if (!isReady()) return null
        database.openIfNeeded()
        return database.findExact(normalizeQuery(wordLower))
    }

    private suspend fun resolveCatalog(): DictCatalogFile {
        val remote = fetchRemoteCatalog().getOrNull()
        if (remote != null) return remote
        return readBundledCatalog()
    }

    private suspend fun fetchRemoteCatalog(): Result<DictCatalogFile> = runCatching {
        val text = http.get(DictDefaults.RemoteCatalogUrl).bodyAsText()
        json.decodeFromString<DictCatalogFile>(text)
    }.onFailure { log.w { "Remote catalog failed: ${it.message}" } }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun readBundledCatalog(): DictCatalogFile {
        val bytes = Res.readBytes("files/dict_dist.json")
        return json.decodeFromString(bytes.decodeToString())
    }

    private suspend fun downloadAndInstall(
        catalog: DictCatalogFile,
        onProgress: (received: Long, total: Long?) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val downloadUrl = sanitizeDownloadUrl(catalog.url)
        val zipPath = dictZipTempPath()
        val stagingPath = dictDbStagingPath()
        log.i { "Streaming dict v${catalog.version} from $downloadUrl" }

        try {
            deleteIfExists(zipPath)
            deleteIfExists(stagingPath)

            http.downloadToFile(downloadUrl, zipPath, onProgress)
            requireValidZipFile(zipPath)

            // Stream unzip to staging file (platform ZipFile / zlib — chunked).
            extractZipEntryToFile(zipPath, DictDefaults.ZipEntryName, stagingPath)

            database.close()
            replaceFile(stagingPath, dictDatabasePath())
            settings.putInt(DictDefaults.SettingsVersionKey, catalog.version)
            settings.putBoolean(DictDefaults.SettingsReadyKey, true)
            database.openIfNeeded()
            log.i { "Dict installed version=${catalog.version} entries≈${catalog.entryCount}" }
        } finally {
            deleteIfExists(zipPath)
            deleteIfExists(stagingPath)
        }
    }
}

/** Fixes mistaken double-scheme URLs like https://https://host/... */
internal fun sanitizeDownloadUrl(raw: String): String {
    var url = raw.trim()
    while (url.startsWith("https://https://", ignoreCase = true)) {
        url = "https://" + url.removePrefix("https://").removePrefix("https://")
    }
    while (url.startsWith("http://http://", ignoreCase = true)) {
        url = "http://" + url.removePrefix("http://").removePrefix("http://")
    }
    return url
}

internal fun requireValidZipFile(path: String) {
    val head = readFilePrefix(path, 4)
    val isZip = head.size >= 4 &&
        head[0] == 0x50.toByte() && head[1] == 0x4B.toByte() &&
        (head[2] == 0x03.toByte() || head[2] == 0x05.toByte() || head[2] == 0x07.toByte())
    if (!isZip) {
        val preview = readFilePrefix(path, 120).decodeToString().replace('\n', ' ')
        error("词典下载失败（不是 zip，可能是错误页）: $preview")
    }
}
