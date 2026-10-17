package com.malism.linsia.typeword.domain

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DictFileParseTest {
    @Test
    fun parsesBundledShape() {
        val json = Json { ignoreUnknownKeys = true }
        val raw = """
            {"id":"demo","title":"Demo","words":[
              {"name":"hello","trans":["你好"],"usphone":"həˈloʊ","ukphone":"həˈləʊ"}
            ]}
        """.trimIndent()
        val file = json.decodeFromString(WordDictFile.serializer(), raw)
        assertEquals("demo", file.id)
        assertEquals("hello", file.words.single().name)
        assertTrue(file.words.single().trans.contains("你好"))
    }
}
