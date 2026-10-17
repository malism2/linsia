package com.malism.linsia.home.quote

import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class DailyQuoteRepository(
    private val http: HttpClient,
    private val settings: Settings,
    private val today: () -> LocalDate = { localToday() },
) {
    private val log = Logger.withTag("DailyQuote")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): DailyQuote {
        val date = today()
        val cached = readCache()
        if (cached != null && cached.date == date.toString()) return cached
        val remote = fetchRemote(date)
        val resolved = resolveDailyQuote(date, cached, remote)
        writeCache(resolved)
        return resolved
    }

    private suspend fun fetchRemote(date: LocalDate): DailyQuote? = runCatching {
        val raw = http.get(DailyQuoteDefaults.ApiUrl) {
            parameter("date", date.toString())
            header(HttpHeaders.UserAgent, "Mozilla/5.0")
            timeout {
                requestTimeoutMillis = 8_000
                connectTimeoutMillis = 5_000
            }
        }.bodyAsText()
        IcibaQuoteParser.parse(raw, date.toString())
            ?: error("empty iciba payload")
    }.onFailure { log.w { "iciba failed: ${it.message}" } }.getOrNull()

    private fun readCache(): DailyQuote? {
        val raw = settings.getStringOrNull(DailyQuoteDefaults.CacheKey) ?: return null
        return runCatching { json.decodeFromString(DailyQuote.serializer(), raw) }.getOrNull()
    }

    private fun writeCache(quote: DailyQuote) {
        settings.putString(
            DailyQuoteDefaults.CacheKey,
            json.encodeToString(DailyQuote.serializer(), quote),
        )
    }
}

internal fun localToday(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
