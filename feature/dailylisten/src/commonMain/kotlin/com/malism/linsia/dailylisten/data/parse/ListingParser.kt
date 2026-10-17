package com.malism.linsia.dailylisten.data.parse

import com.malism.linsia.dailylisten.domain.ListingCandidate

private const val Host = "https://www.21voa.com"

private val ArticleHref = Regex(
    """href=["']([^"']*special_english/[^"']+-(\d+)\.html)["']""",
    RegexOption.IGNORE_CASE,
)

private val LiBlock = Regex(
    """<li\b[^>]*>(.*?)</li>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)

private val ShortDate = Regex("""\((\d{2}-\d{1,2}-\d{1,2})\)""")

fun parseListingHtml(html: String): List<ListingCandidate> {
    val seen = mutableSetOf<String>()
    return LiBlock.findAll(html).mapNotNull { match ->
        parseListingBlock(match.groupValues[1], seen)
    }.toList()
}

private fun parseListingBlock(block: String, seen: MutableSet<String>): ListingCandidate? {
    val hrefMatch = ArticleHref.find(block) ?: return null
    val id = hrefMatch.groupValues[2]
    if (!seen.add(id)) return null
    val title = listingTitle(block) ?: return null
    val dateRaw = ShortDate.find(block)?.groupValues?.get(1).orEmpty()
    return ListingCandidate(
        id = id,
        title = title,
        publishedAt = formatFlexibleDate(dateRaw),
        publishedEpoch = parseFlexibleEpoch(dateRaw),
        sourceUrl = absoluteUrl(hrefMatch.groupValues[1]),
    )
}

internal fun listingTitle(block: String): String? {
    val anchors = Regex(
        """<a\b([^>]*)>(.*?)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    for (anchor in anchors.findAll(block)) {
        if (!ArticleHref.containsMatchIn(anchor.groupValues[1])) continue
        val title = stripTags(anchor.groupValues[2]).trim()
        if (isListingTitle(title)) return title
    }
    return null
}

internal fun isListingTitle(title: String): Boolean {
    if (title.length < 2) return false
    val compact = title.replace("&nbsp;", " ").trim()
    if (compact.isEmpty()) return false
    if (compact.all { it.isDigit() }) return false
    val lower = compact.lowercase()
    if (lower == "next" || lower == "prev" || lower == "previous" || lower == "lrc") return false
    if (lower.contains("&gt;") || lower.contains("&lt;")) return false
    if (compact.all { it == '>' || it == '<' || it == '|' }) return false
    return true
}

internal fun absoluteUrl(href: String): String = when {
    href.startsWith("https://") || href.startsWith("http://") -> href
    href.startsWith("//") -> "https:$href"
    href.startsWith("/") -> Host + href
    else -> "$Host/$href"
}
