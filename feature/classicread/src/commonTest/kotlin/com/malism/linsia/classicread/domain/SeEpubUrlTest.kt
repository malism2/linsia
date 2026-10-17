package com.malism.linsia.classicread.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeEpubUrlTest {
    @Test
    fun appendsSourceDownloadQuery() {
        val url = "https://standardebooks.org/ebooks/a/b/downloads/a_b.epub"
        assertEquals("$url?source=download", url.ensureSeEpubDownloadUrl())
    }

    @Test
    fun keepsExistingSourceDownload() {
        val url = "https://standardebooks.org/ebooks/a/b/downloads/a_b.epub?source=download"
        assertEquals(url, url.ensureSeEpubDownloadUrl())
    }

    @Test
    fun ignoresNonSeUrls() {
        assertEquals("", "".ensureSeEpubDownloadUrl())
        assertEquals("https://example.com/book.epub", "https://example.com/book.epub".ensureSeEpubDownloadUrl())
    }
}
