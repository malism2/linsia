package com.malism.linsia.classicread.data

import com.malism.linsia.classicread.domain.SeBooksCatalogFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class SeBooksCatalogTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesSeBooksCatalog() {
        val raw = """
            {
              "version": 1,
              "updatedAt": "2026-09-01",
              "source": "standardebooks.org/ebooks",
              "books": [{
                "id": "eleanor-h-porter/pollyanna",
                "rank": 1,
                "title": "Pollyanna",
                "author": "Eleanor H. Porter",
                "coverUrl": "https://standardebooks.org/images/covers/cover.jpg",
                "epubUrl": "https://standardebooks.org/ebooks/eleanor-h-porter/pollyanna/downloads/eleanor-h-porter_pollyanna.epub"
              }]
            }
        """.trimIndent()
        val catalog = json.decodeFromString<SeBooksCatalogFile>(raw)
        assertEquals(1, catalog.version)
        assertEquals(1, catalog.books.size)
        assertEquals("eleanor-h-porter/pollyanna", catalog.books.first().id)
        assertTrue(catalog.books.first().epubUrl.endsWith(".epub"))
    }
}
