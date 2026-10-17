package com.malism.linsia.dailylisten.data.parse

private val HtmlTag = Regex("""<[^>]+>""")
private val Whitespace = Regex("""[ \t\x0B\f\r]+""")

fun stripTags(html: String): String =
    decodeXml(html.replace(HtmlTag, " "))
        .replace('\n', ' ')
        .replace(Whitespace, " ")
        .trim()

fun decodeXml(value: String): String {
    var text = unwrapCdata(value)
    text = text.replace("&nbsp;", " ")
        .replace("&#160;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&#x27;", "'")
        .replace("&#8217;", "’")
        .replace("&#8216;", "‘")
        .replace("&#8220;", "“")
        .replace("&#8221;", "”")
        .replace("&#8211;", "–")
        .replace("&#8212;", "—")
        .replace("&rsquo;", "’")
        .replace("&lsquo;", "‘")
        .replace("&rdquo;", "”")
        .replace("&ldquo;", "“")
        .replace("&mdash;", "—")
        .replace("&ndash;", "–")
    text = numericEntity.replace(text) { match ->
        val hex = match.groupValues[1]
        val dec = match.groupValues[2]
        val code = when {
            hex.isNotEmpty() -> hex.toIntOrNull(16)
            else -> dec.toIntOrNull()
        } ?: return@replace match.value
        code.toChar().toString()
    }
    return text
}

private val numericEntity = Regex("""&#x([0-9a-fA-F]+);|&#(\d+);""")

private val Months = mapOf(
    "jan" to 1, "january" to 1,
    "feb" to 2, "february" to 2,
    "mar" to 3, "march" to 3,
    "apr" to 4, "april" to 4,
    "may" to 5,
    "jun" to 6, "june" to 6,
    "jul" to 7, "july" to 7,
    "aug" to 8, "august" to 8,
    "sep" to 9, "september" to 9,
    "oct" to 10, "october" to 10,
    "nov" to 11, "november" to 11,
    "dec" to 12, "december" to 12,
)

fun formatFlexibleDate(raw: String): String {
    val parsed = parseDate(raw) ?: return raw.trim()
    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )
    return "${monthNames[parsed.month - 1]} ${parsed.day}, ${parsed.year}"
}

fun parseFlexibleEpoch(raw: String): Long {
    val parsed = parseDate(raw) ?: return 0L
    return daysToEpoch(parsed.year, parsed.month, parsed.day) * 86_400L +
        parsed.hour * 3_600L + parsed.minute * 60L + parsed.second
}

fun epochToDateIso(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return "1970-01-01"
    val days = epochSeconds / 86_400L
    val ymd = epochDaysToYmd(days)
    val y = ymd.first.toString().padStart(4, '0')
    val m = ymd.second.toString().padStart(2, '0')
    val d = ymd.third.toString().padStart(2, '0')
    return "$y-$m-$d"
}

internal fun unwrapCdata(value: String): String {
    val regex = Regex("""<!\[CDATA\[(.*?)]]>""", RegexOption.DOT_MATCHES_ALL)
    return regex.find(value)?.groupValues?.get(1) ?: value
}

internal fun mp3Url(html: String): String? {
    val regex = Regex("""https?://[^"'<\s>]+\.mp3""", RegexOption.IGNORE_CASE)
    return regex.find(html)?.value
}

private data class ParsedDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
)

private fun parseDate(raw: String): ParsedDate? {
    val text = raw.trim()
    val iso = Regex(
        """(\d{4})-(\d{2})-(\d{2})(?:[ T](\d{2}):(\d{2})(?::(\d{2}))?)?""",
    ).find(text)
    if (iso != null) {
        return ParsedDate(
            year = iso.groupValues[1].toInt(),
            month = iso.groupValues[2].toInt(),
            day = iso.groupValues[3].toInt(),
            hour = iso.groupValues[4].toIntOrNull() ?: 0,
            minute = iso.groupValues[5].toIntOrNull() ?: 0,
            second = iso.groupValues[6].toIntOrNull() ?: 0,
        )
    }
    val rfc = Regex(
        """(\d{1,2})\s+([A-Za-z]{3,9})\s+(\d{4})(?:\s+(\d{2}):(\d{2})(?::(\d{2}))?)?""",
    ).find(text)
    if (rfc != null) {
        val month = Months[rfc.groupValues[2].lowercase()] ?: return null
        return ParsedDate(
            year = rfc.groupValues[3].toInt(),
            month = month,
            day = rfc.groupValues[1].toInt(),
            hour = rfc.groupValues[4].toIntOrNull() ?: 0,
            minute = rfc.groupValues[5].toIntOrNull() ?: 0,
            second = rfc.groupValues[6].toIntOrNull() ?: 0,
        )
    }
    val longDate = Regex(
        """([A-Za-z]+)\s+(\d{1,2}),\s+(\d{4})""",
    ).find(text)
    if (longDate != null) {
        val month = Months[longDate.groupValues[1].lowercase()] ?: return null
        return ParsedDate(
            year = longDate.groupValues[3].toInt(),
            month = month,
            day = longDate.groupValues[2].toInt(),
            hour = 0,
            minute = 0,
            second = 0,
        )
    }
    val short = Regex("""^(\d{2})-(\d{1,2})-(\d{1,2})$""").find(text) ?: return null
    val year = 2000 + short.groupValues[1].toInt()
    return ParsedDate(
        year = year,
        month = short.groupValues[2].toInt(),
        day = short.groupValues[3].toInt(),
        hour = 0,
        minute = 0,
        second = 0,
    )
}

private fun daysToEpoch(year: Int, month: Int, day: Int): Long {
    val y = if (month <= 2) year - 1 else year
    val m = if (month <= 2) month + 9 else month - 3
    val era = if (y >= 0) y / 400 else (y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * m + 2) / 5 + day - 1
    val doe = yoe * 365L + yoe / 4 - yoe / 100 + doy
    return era * 146097L + doe - 719468L
}

private fun epochDaysToYmd(epochDay: Long): Triple<Int, Int, Int> {
    var z = epochDay + 719468L
    val era = if (z >= 0) z / 146097L else (z - 146096L) / 146097L
    val doe = z - era * 146097L
    val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
    val y = (yoe + era * 400L).toInt()
    val doy = (doe - (365L * yoe + yoe / 4L - yoe / 100L)).toInt()
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    val year = if (m <= 2) y + 1 else y
    return Triple(year, m, d)
}
