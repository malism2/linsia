package com.malism.linsia.classicread.data.epub

import java.io.File
import java.util.zip.ZipFile

actual class EpubZipReader actual constructor(private val path: String) : AutoCloseable {
    private val zip = ZipFile(File(path))

    actual fun readEntry(entryName: String): ByteArray {
        val entry = zip.getEntry(entryName)
            ?: zip.entries().asSequence().firstOrNull { matchesEntry(it.name, entryName) }
            ?: error("Zip missing $entryName")
        zip.getInputStream(entry).use { input ->
            return input.readBytes()
        }
    }

    actual override fun close() {
        zip.close()
    }

    private fun matchesEntry(actualName: String, requested: String): Boolean {
        return actualName == requested || actualName.endsWith("/$requested")
    }
}
