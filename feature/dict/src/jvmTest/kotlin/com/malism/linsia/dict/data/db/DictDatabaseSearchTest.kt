package com.malism.linsia.dict.data.db

import com.malism.linsia.dict.data.fs.dictDatabasePath
import com.malism.linsia.dict.data.zip.extractZipEntryToFile
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DictDatabaseSearchTest {
    private val db = DictDatabase()
    private val sampleZip: File? = findRepoFile("dist/world/ecdict.db.zip")

    @BeforeTest
    fun setUp() = runBlocking {
        val zip = sampleZip ?: return@runBlocking
        extractZipEntryToFile(zip.absolutePath, "ecdict.db", dictDatabasePath())
        db.openIfNeeded()
    }

    @AfterTest
    fun tearDown() = runBlocking {
        db.close()
    }

    @Test
    fun exactAndPrefixSearch() = runBlocking {
        if (sampleZip == null) return@runBlocking
        val exact = db.findExact("abandon")
        assertNotNull(exact)
        assertEquals("abandon", exact.wordLower)
        assertTrue(exact.translation.contains("放弃"))

        val prefix = db.searchPrefix("ab")
        assertTrue(prefix.isNotEmpty())
        assertTrue(prefix.any { it.wordLower.startsWith("ab") })
    }

    private fun findRepoFile(relative: String): File? {
        var dir: File? = File(".").absoluteFile
        while (dir != null) {
            val candidate = File(dir, relative)
            if (candidate.isFile) return candidate
            dir = dir.parentFile
        }
        return null
    }
}
