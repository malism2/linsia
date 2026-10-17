package com.malism.linsia.classicread.data.epub

import com.malism.linsia.classicread.domain.ParsedChapter

private val SKIP_HREF = Regex("""(colophon|imprint|titlepage|uncopyright|half-title)""", RegexOption.IGNORE_CASE)
private val ATTR = Regex("""(\w[\w:-]*)\s*=\s*"([^"]*)"""")
private val TAG = Regex("""</?\s*([a-zA-Z][\w:-]*)""")

object EpubParser {
    fun parse(epubPath: String): List<ParsedChapter> {
        EpubZipReader(epubPath).use { zip ->
            val container = zip.readEntry("META-INF/container.xml").decodeToString()
            val opfPath = Regex("""full-path="([^"]+)"""")
                .find(container)
                ?.groupValues
                ?.get(1)
                ?: error("container.xml missing full-path")
            val opf = zip.readEntry(opfPath).decodeToString()
            val opfDir = opfPath.substringBeforeLast('/', missingDelimiterValue = "")
            val manifest = parseManifest(opf)
            val spineIds = parseSpine(opf)
            val chapters = mutableListOf<ParsedChapter>()
            var chapterNumber = 0
            for (idref in spineIds) {
                val href = manifest[idref] ?: continue
                if (SKIP_HREF.containsMatchIn(href)) continue
                val entryPath = resolvePath(opfDir, href)
                val xhtml = runCatching { zip.readEntry(entryPath).decodeToString() }.getOrNull() ?: continue
                val (title, body) = xhtmlToChapter(xhtml, ++chapterNumber)
                if (body.isBlank()) continue
                chapters += ParsedChapter(title = title, body = body)
            }
            return chapters
        }
    }

    private fun parseManifest(opf: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val itemRegex = Regex("""<item\b([^>]*)/?>""", RegexOption.IGNORE_CASE)
        for (match in itemRegex.findAll(opf)) {
            val attrs = parseAttrs(match.groupValues[1])
            val id = attrs["id"] ?: continue
            val href = attrs["href"] ?: continue
            val mediaType = attrs["media-type"].orEmpty()
            if (mediaType.isNotBlank() && !mediaType.contains("html")) continue
            result[id] = href
        }
        return result
    }

    private fun parseSpine(opf: String): List<String> {
        val spineMatch = Regex("""<spine\b[^>]*>(.*?)</spine>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(opf)
            ?.groupValues
            ?.get(1)
            ?: return emptyList()
        return Regex("""<itemref\b([^>]*)/?>""", RegexOption.IGNORE_CASE)
            .findAll(spineMatch)
            .mapNotNull { parseAttrs(it.groupValues[1])["idref"] }
            .toList()
    }

    private fun parseAttrs(fragment: String): Map<String, String> {
        val attrs = mutableMapOf<String, String>()
        for (match in ATTR.findAll(fragment)) {
            attrs[match.groupValues[1]] = decodeXmlEntities(match.groupValues[2])
        }
        return attrs
    }

    private fun resolvePath(baseDir: String, href: String): String {
        val combined = if (baseDir.isEmpty()) href else "$baseDir/$href"
        val parts = combined.split('/')
        val stack = ArrayDeque<String>()
        for (part in parts) {
            when (part) {
                "", "." -> Unit
                ".." -> if (stack.isNotEmpty()) stack.removeLast()
                else -> stack.addLast(part)
            }
        }
        return stack.joinToString("/")
    }

    private fun xhtmlToChapter(xhtml: String, chapterNumber: Int): Pair<String, String> {
        val withoutScripts = xhtml
            .replace(Regex("""<script\b[^>]*>.*?</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("""<style\b[^>]*>.*?</style>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
        val heading = Regex("""<h[23][^>]*>(.*?)</h[23]>""", RegexOption.IGNORE_CASE)
            .find(withoutScripts)
            ?.groupValues
            ?.get(1)
            ?.let(::stripTags)
            ?.trim()
            .orEmpty()
        val paragraphs = Regex("""<p\b[^>]*>(.*?)</p>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .findAll(withoutScripts)
            .map { stripTags(it.groupValues[1]).trim() }
            .filter { it.isNotEmpty() }
            .toList()
        val body = if (paragraphs.isNotEmpty()) {
            paragraphs.joinToString("\n\n")
        } else {
            stripTags(withoutScripts).replace(Regex("""\n{3,}"""), "\n\n").trim()
        }
        val title = heading.ifBlank { "Chapter $chapterNumber" }
        return title to body
    }

    private fun stripTags(html: String): String =
        decodeXmlEntities(html.replace(Regex("<[^>]+>"), " "))
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun decodeXmlEntities(text: String): String =
        text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace(Regex("""&#(\d+);""")) { it.groupValues[1].toInt().toChar().toString() }
}
