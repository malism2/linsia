package com.malism.linsia.classicread.domain

data class PaginatedBook(
    val paragraphs: List<String>,
    val paragraphPageIndex: List<Int>,
    val pageCount: Int,
)

object BookPagination {
    const val CharsPerPage = 2_000

    fun build(chapterBodies: List<String>, charsPerPage: Int = CharsPerPage): PaginatedBook {
        val paragraphs = chapterBodies
            .flatMap { body -> body.split("\n\n") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (paragraphs.isEmpty()) return PaginatedBook(emptyList(), emptyList(), 0)

        val pageTexts = mutableListOf<String>()
        val paragraphPageIndex = IntArray(paragraphs.size)
        val current = StringBuilder()

        for (index in paragraphs.indices) {
            paragraphPageIndex[index] = appendParagraph(current, pageTexts, paragraphs[index], charsPerPage)
        }
        if (current.isNotEmpty()) {
            pageTexts += current.toString()
        }
        return PaginatedBook(
            paragraphs = paragraphs,
            paragraphPageIndex = paragraphPageIndex.toList(),
            pageCount = pageTexts.size,
        )
    }

    private fun appendParagraph(
        current: StringBuilder,
        pages: MutableList<String>,
        paragraph: String,
        charsPerPage: Int,
    ): Int {
        if (paragraph.length > charsPerPage) {
            val startPage = pages.size
            if (current.isNotEmpty()) {
                pages += current.toString()
                current.clear()
            }
            var offset = 0
            while (offset < paragraph.length) {
                val end = (offset + charsPerPage).coerceAtMost(paragraph.length)
                pages += paragraph.substring(offset, end)
                offset = end
            }
            return startPage
        }

        val addition = if (current.isEmpty()) paragraph else "\n\n$paragraph"
        if (current.isNotEmpty() && current.length + addition.length > charsPerPage) {
            val startPage = pages.size + 1
            pages += current.toString()
            current.clear()
            current.append(paragraph)
            return startPage
        }

        val startPage = pages.size
        if (current.isNotEmpty()) current.append("\n\n")
        current.append(paragraph)
        return startPage
    }
}
