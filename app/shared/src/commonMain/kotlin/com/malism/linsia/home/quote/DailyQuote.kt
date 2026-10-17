package com.malism.linsia.home.quote

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class DailyQuote(
    val english: String,
    val chinese: String,
    val ttsUrl: String? = null,
    val date: String,
    val fromNetwork: Boolean,
)

object DailyQuoteDefaults {
    const val ApiUrl = "https://open.iciba.com/dsapi/"
    const val CacheKey = "home.dailyQuote"
    const val AttributionRemote = "金山词霸每日一句"
    const val AttributionLocal = "本地词库"
}

fun pickFallbackQuote(date: LocalDate): DailyQuote {
    val quotes = FallbackQuotes.all
    val index = (date.dayOfYear - 1).mod(quotes.size)
    val item = quotes[index]
    return DailyQuote(
        english = item.english,
        chinese = item.chinese,
        ttsUrl = youdaoSentenceTts(item.english),
        date = date.toString(),
        fromNetwork = false,
    )
}

fun resolveDailyQuote(
    today: LocalDate,
    cached: DailyQuote?,
    remote: DailyQuote?,
): DailyQuote {
    val todayText = today.toString()
    if (cached != null && cached.date == todayText) return cached
    if (remote != null) return remote
    if (cached != null) return cached
    return pickFallbackQuote(today)
}

fun youdaoSentenceTts(english: String): String {
    val query = buildString {
        english.forEach { ch ->
            when {
                ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch == '-' || ch == '_' || ch == '.' || ch == '~' -> append(ch)
                ch == ' ' -> append('+')
                else -> {
                    val bytes = ch.toString().encodeToByteArray()
                    bytes.forEach { b ->
                        val unsigned = b.toInt() and 0xFF
                        append('%')
                        append((unsigned ushr 4).toString(16).uppercase())
                        append((unsigned and 0xF).toString(16).uppercase())
                    }
                }
            }
        }
    }
    return "https://dict.youdao.com/dictvoice?audio=$query&type=2"
}
