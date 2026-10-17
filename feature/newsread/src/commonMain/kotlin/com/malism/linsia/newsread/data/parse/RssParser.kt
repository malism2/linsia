package com.malism.linsia.newsread.data.parse

data class RssItem(
    val id: String,
    val title: String,
    val summary: String,
    val publishedAt: String,
    val publishedEpoch: Long,
    val sourceUrl: String,
    val audioUrl: String?,
)

fun parseRssItems(xml: String): List<RssItem> {
    val items = itemBlocks(xml)
    return items.mapNotNull { block ->
        val link = firstTag(block, "link") ?: firstTag(block, "guid") ?: return@mapNotNull null
        val id = articleIdFromUrl(link) ?: return@mapNotNull null
        val title = decodeXml(firstTag(block, "title").orEmpty()).ifBlank { return@mapNotNull null }
        val description = decodeXml(firstTag(block, "description").orEmpty())
        val pubDate = firstTag(block, "pubDate").orEmpty()
        RssItem(
            id = id,
            title = title,
            summary = stripTags(description).trim(),
            publishedAt = formatPubDate(pubDate),
            publishedEpoch = parsePubDateEpoch(pubDate),
            sourceUrl = link.trim(),
            audioUrl = enclosureUrl(block) ?: mp3Url(block),
        )
    }
}

fun articleIdFromUrl(url: String): String? {
    val twentyOne = Regex(
        """special_english/[^/"']+-(\d+)\.html""",
        RegexOption.IGNORE_CASE,
    ).find(url)
    if (twentyOne != null) return twentyOne.groupValues[1]
    val chinaDaily = Regex("""/(WS[0-9a-fA-F]+)\.html""", RegexOption.IGNORE_CASE).find(url)
    if (chinaDaily != null) return chinaDaily.groupValues[1]
    val voa = Regex("""/a/(?:[^/]+/)?(\d+)\.html""").find(url) ?: return null
    return voa.groupValues[1]
}

internal fun itemBlocks(xml: String): List<String> {
    val regex = Regex("""<item\b[^>]*>(.*?)</item>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    return regex.findAll(xml).map { it.groupValues[1] }.toList()
}

internal fun firstTag(block: String, tag: String): String? {
    val regex = Regex("""<$tag\b[^>]*>(.*?)</$tag>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    val raw = regex.find(block)?.groupValues?.get(1)?.trim() ?: return null
    return unwrapCdata(raw).trim().takeIf { it.isNotEmpty() }
}

internal fun enclosureUrl(block: String): String? {
    val regex = Regex("""<enclosure\b[^>]*url=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    return regex.find(block)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
}

internal fun mp3Url(block: String): String? {
    val regex = Regex("""https?://[^"'<\s>]+\.mp3""", RegexOption.IGNORE_CASE)
    return regex.find(block)?.value
}

internal fun unwrapCdata(value: String): String {
    val regex = Regex("""<!\[CDATA\[(.*?)]]>""", RegexOption.DOT_MATCHES_ALL)
    return regex.find(value)?.groupValues?.get(1) ?: value
}
