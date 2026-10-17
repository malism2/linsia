package com.malism.linsia.home.quote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val NumericEntity = Regex("""&#x([0-9a-fA-F]+);|&#(\d+);""", setOf(RegexOption.IGNORE_CASE))

@Serializable
internal data class IcibaQuoteDto(
    val content: String? = null,
    val note: String? = null,
    val tts: String? = null,
    val dateline: String? = null,
)

object IcibaQuoteParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(raw: String, today: String): DailyQuote? {
        val dto = runCatching { json.decodeFromString(IcibaQuoteDto.serializer(), raw) }.getOrNull()
            ?: return null
        val english = decodeEntities(dto.content.orEmpty()).trim()
        val chinese = decodeEntities(dto.note.orEmpty()).trim()
        if (english.isEmpty() || chinese.isEmpty()) return null
        val tts = dto.tts?.trim()?.takeIf { it.startsWith("http") }
        return DailyQuote(
            english = english,
            chinese = chinese,
            ttsUrl = tts,
            date = today,
            fromNetwork = true,
        )
    }
}

internal fun decodeEntities(value: String): String {
    var text = value
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&ldquo;", "“")
        .replace("&rdquo;", "”")
        .replace("&lsquo;", "‘")
        .replace("&rsquo;", "’")
    text = NumericEntity.replace(text) { match ->
        val hex = match.groupValues[1]
        val dec = match.groupValues[2]
        val code = when {
            hex.isNotEmpty() -> hex.toIntOrNull(16)
            else -> dec.toIntOrNull()
        } ?: return@replace match.value
        if (code in 1..0xFFFF) code.toChar().toString() else match.value
    }
    return text
}
