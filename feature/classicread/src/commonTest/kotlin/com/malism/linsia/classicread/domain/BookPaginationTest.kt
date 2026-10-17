package com.malism.linsia.classicread.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class BookPaginationTest {
    @Test
    fun keepsParagraphsContinuous() {
        val content = BookPagination.build(
            listOf("Paragraph one.", "Paragraph two.", "Paragraph three."),
            charsPerPage = 2_000,
        )
        assertEquals(3, content.paragraphs.size)
        assertEquals(1, content.pageCount)
        assertEquals(listOf(0, 0, 0), content.paragraphPageIndex)
    }

    @Test
    fun tracksPageWhenContentOverflows() {
        val content = BookPagination.build(
            listOf("Paragraph one.", "Paragraph two.", "Paragraph three."),
            charsPerPage = 45,
        )
        assertEquals(3, content.paragraphs.size)
        assertEquals(2, content.pageCount)
        assertEquals(listOf(0, 0, 1), content.paragraphPageIndex)
    }

    @Test
    fun splitsLongParagraphIntoPages() {
        val long = "a".repeat(50)
        val content = BookPagination.build(listOf(long), charsPerPage = 20)
        assertEquals(3, content.pageCount)
        assertEquals(0, content.paragraphPageIndex.single())
    }
}
