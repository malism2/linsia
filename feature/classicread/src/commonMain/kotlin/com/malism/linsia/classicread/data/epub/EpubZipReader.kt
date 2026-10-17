package com.malism.linsia.classicread.data.epub

expect class EpubZipReader(path: String) : AutoCloseable {
    fun readEntry(entryName: String): ByteArray
    override fun close()
}
