package com.malism.linsia.dict.data.zip

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

internal data class LocatedZipEntry(
    val name: String,
    val method: Int,
    val compressedSize: Long,
    val localHeaderOffset: Long,
)

/**
 * Locates a zip entry by reading only the EOCD + central directory (not the file payload).
 */
internal fun locateZipEntry(zipPath: String, entryName: String): LocatedZipEntry {
    val fs = FileSystem.SYSTEM
    val path = zipPath.toPath()
    val size = fs.metadata(path).size ?: error("Empty zip: $zipPath")
    require(size >= 22) { "Zip too small" }

    val tailLen = minOf(65_557L, size)
    val tail = ByteArray(tailLen.toInt())
    fs.source(path).buffer().use { source ->
        source.skip(size - tailLen)
        source.readFully(tail)
    }

    val eocdRel = findEocdOffset(tail) ?: error("Invalid zip: EOCD not found")
    val cdSize = readInt(tail, eocdRel + 12).toLong() and 0xFFFF_FFFFL
    val cdOffset = readInt(tail, eocdRel + 16).toLong() and 0xFFFF_FFFFL

    val cd = ByteArray(cdSize.toInt())
    fs.source(path).buffer().use { source ->
        source.skip(cdOffset)
        source.readFully(cd)
    }

    var pos = 0
    while (pos + 46 <= cd.size) {
        if (readInt(cd, pos) != 0x02014b50) break
        val method = readShort(cd, pos + 10)
        val compSize = readInt(cd, pos + 20).toLong() and 0xFFFF_FFFFL
        val nameLen = readShort(cd, pos + 28)
        val extraLen = readShort(cd, pos + 30)
        val commentLen = readShort(cd, pos + 32)
        val localOffset = readInt(cd, pos + 42).toLong() and 0xFFFF_FFFFL
        val name = cd.decodeToString(pos + 46, pos + 46 + nameLen)
        if (name == entryName || name.endsWith(entryName)) {
            return LocatedZipEntry(
                name = name,
                method = method,
                compressedSize = compSize,
                localHeaderOffset = localOffset,
            )
        }
        pos += 46 + nameLen + extraLen + commentLen
    }
    error("Zip missing $entryName")
}

internal fun localDataOffset(zipPath: String, localHeaderOffset: Long): Long {
    val header = ByteArray(30)
    FileSystem.SYSTEM.source(zipPath.toPath()).buffer().use { source ->
        source.skip(localHeaderOffset)
        source.readFully(header)
    }
    require(readInt(header, 0) == 0x04034b50) { "Bad local header" }
    val nameLen = readShort(header, 26)
    val extraLen = readShort(header, 28)
    return localHeaderOffset + 30 + nameLen + extraLen
}

private fun findEocdOffset(bytes: ByteArray): Int? {
    val min = (bytes.size - 22 - 0xFFFF).coerceAtLeast(0)
    for (i in bytes.size - 22 downTo min) {
        if (readInt(bytes, i) == 0x06054b50) return i
    }
    return null
}

private fun readInt(bytes: ByteArray, offset: Int): Int {
    val b0 = bytes[offset].toInt() and 0xFF
    val b1 = bytes[offset + 1].toInt() and 0xFF
    val b2 = bytes[offset + 2].toInt() and 0xFF
    val b3 = bytes[offset + 3].toInt() and 0xFF
    return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
}

private fun readShort(bytes: ByteArray, offset: Int): Int {
    val b0 = bytes[offset].toInt() and 0xFF
    val b1 = bytes[offset + 1].toInt() and 0xFF
    return b0 or (b1 shl 8)
}
