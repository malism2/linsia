package com.malism.linsia.dailylisten.data.parse

data class ParsedArticle(
    val body: String,
    val audioUrl: String?,
)

fun parseArticleHtml(html: String): ParsedArticle {
    val section = contentSection(html)
    val paragraphs = paragraphTexts(section.ifBlank { html })
        .filterNot { isBoilerplate(it) }
    return ParsedArticle(
        body = paragraphs.joinToString("\n\n"),
        audioUrl = mp3Url(html),
    )
}

internal fun contentSection(html: String): String {
    val righter = Regex(
        """<div[^>]*id=["']righter["'][^>]*>""",
        RegexOption.IGNORE_CASE,
    ).find(html)
    val searchFrom = righter?.range?.last?.plus(1) ?: 0
    val content = Regex(
        """<div\b[^>]*class=["'][^"']*\bcontent\b[^"']*["'][^>]*>""",
        RegexOption.IGNORE_CASE,
    ).find(html, searchFrom) ?: Regex(
        """<(?:div|td|article)\b[^>]*id=["']Content["'][^>]*>""",
        RegexOption.IGNORE_CASE,
    ).find(html)
    if (content != null) return innerHtml(html, content.range.last + 1)
    return articleSection(html)
}

internal fun articleSection(html: String): String {
    val wsw = Regex(
        """<div[^>]*class=["'][^"']*\bwsw\b[^"']*["'][^>]*>""",
        RegexOption.IGNORE_CASE,
    ).find(html) ?: return html
    return innerHtml(html, wsw.range.last + 1)
}

internal fun paragraphTexts(html: String): List<String> {
    val regex = Regex("""<p\b[^>]*>(.*?)</p>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    return regex.findAll(html).map { stripTags(it.groupValues[1]) }.filter { it.isNotBlank() }.toList()
}

internal fun innerHtml(html: String, start: Int): String {
    var depth = 1
    var i = start
    val lower = html.lowercase()
    while (i < html.length && depth > 0) {
        val nextOpen = lower.indexOf("<div", i)
        val nextClose = lower.indexOf("</div", i)
        when {
            nextClose < 0 -> return html.substring(start)
            nextOpen in 0 until nextClose -> {
                depth++
                i = nextOpen + 4
            }
            else -> {
                depth--
                if (depth == 0) return html.substring(start, nextClose)
                i = nextClose + 5
            }
        }
    }
    return html.substring(start)
}

private fun isBoilerplate(text: String): Boolean {
    val compact = text.replace('\u00A0', ' ').trim()
    if (compact.length <= 1) return true
    val lower = compact.lowercase()
    return lower.startsWith("download") ||
        lower.startsWith("share") ||
        lower.startsWith("embed") ||
        lower == "see comments" ||
        lower.startsWith("related stories") ||
        lower.startsWith("photo/") ||
        compact.startsWith("中国日报") ||
        compact == "来源" ||
        compact.equals("go top", ignoreCase = true) ||
        compact.startsWith("Words in This Story")
}
