package com.malism.linsia.home.quote

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DailyQuoteTest {
    @Test
    fun parseIcibaPayload() {
        val raw = """
            {"sid":"6034","tts":"https://staticedu-wps-cache.iciba.com/audio/demo.mp3",
             "content":"Your silent march through storms writes peace upon our land.",
             "note":"你穿越风暴的无声行进，在大地写下和平。",
             "dateline":"2026-08-01"}
        """.trimIndent()
        val quote = IcibaQuoteParser.parse(raw, "2026-08-19")
        assertNotNull(quote)
        assertTrue(quote.fromNetwork)
        assertEquals("2026-08-19", quote.date)
        assertEquals("Your silent march through storms writes peace upon our land.", quote.english)
        assertEquals("你穿越风暴的无声行进，在大地写下和平。", quote.chinese)
        assertEquals("https://staticedu-wps-cache.iciba.com/audio/demo.mp3", quote.ttsUrl)
    }

    @Test
    fun decodeNumericEntities() {
        val raw = """
            {"content":"Hello","note":"你好&#xff0c;世界"}
        """.trimIndent()
        val quote = IcibaQuoteParser.parse(raw, "2026-01-01")
        assertEquals("你好，世界", quote?.chinese)
    }

    @Test
    fun emptyContentIsRejected() {
        assertEquals(null, IcibaQuoteParser.parse("""{"content":"","note":"中文"}""", "2026-01-01"))
        assertEquals(null, IcibaQuoteParser.parse("not-json", "2026-01-01"))
    }

    @Test
    fun cachedTodayWins() {
        val today = LocalDate(2026, 8, 19)
        val cached = DailyQuote("cached", "缓存", date = "2026-08-19", fromNetwork = true)
        val remote = DailyQuote("remote", "远程", date = "2026-08-19", fromNetwork = true)
        assertEquals(cached, resolveDailyQuote(today, cached, remote))
    }

    @Test
    fun remoteUsedWhenCacheStale() {
        val today = LocalDate(2026, 8, 19)
        val cached = DailyQuote("cached", "缓存", date = "2026-08-18", fromNetwork = true)
        val remote = DailyQuote("remote", "远程", date = "2026-08-19", fromNetwork = true)
        assertEquals(remote, resolveDailyQuote(today, cached, remote))
    }

    @Test
    fun staleCacheWhenRemoteMissing() {
        val today = LocalDate(2026, 8, 19)
        val cached = DailyQuote("cached", "缓存", date = "2026-08-18", fromNetwork = true)
        assertEquals(cached, resolveDailyQuote(today, cached, remote = null))
    }

    @Test
    fun fallbackWhenNothingCached() {
        val today = LocalDate(2026, 1, 1)
        val quote = resolveDailyQuote(today, cached = null, remote = null)
        assertFalse(quote.fromNetwork)
        assertEquals(today.toString(), quote.date)
        assertEquals(FallbackQuotes.all.first().english, quote.english)
        assertTrue(quote.ttsUrl.orEmpty().startsWith("https://dict.youdao.com/dictvoice"))
    }

    @Test
    fun fallbackWrapsByDayOfYear() {
        val first = pickFallbackQuote(LocalDate(2026, 1, 1))
        val next = pickFallbackQuote(LocalDate(2026, 1, 2))
        val wrapped = pickFallbackQuote(LocalDate(2026, 1, 1).plus(DatePeriod(days = FallbackQuotes.all.size)))
        assertEquals(FallbackQuotes.all[0].english, first.english)
        assertEquals(FallbackQuotes.all[1].english, next.english)
        assertEquals(first.english, wrapped.english)
    }
}
