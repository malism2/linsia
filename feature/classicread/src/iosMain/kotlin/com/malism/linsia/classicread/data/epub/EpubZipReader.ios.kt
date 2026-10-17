package com.malism.linsia.classicread.data.epub

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import platform.zlib.Z_NO_FLUSH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2
import platform.zlib.z_stream

@OptIn(ExperimentalForeignApi::class)
actual class EpubZipReader actual constructor(private val path: String) : AutoCloseable {
    actual fun readEntry(entryName: String): ByteArray {
        val entry = locateZipEntry(path, entryName)
        val dataOffset = localDataOffset(path, entry.localHeaderOffset)
        return when (entry.method) {
            0 -> readStored(path, dataOffset, entry.compressedSize)
            8 -> readDeflated(path, dataOffset, entry.compressedSize)
            else -> error("Unsupported zip method ${entry.method}")
        }
    }

    actual override fun close() = Unit
}

private data class LocatedZipEntry(
    val name: String,
    val method: Int,
    val compressedSize: Long,
    val localHeaderOffset: Long,
)

private fun locateZipEntry(zipPath: String, entryName: String): LocatedZipEntry {
    val fs = FileSystem.SYSTEM
    val filePath = zipPath.toPath()
    val size = fs.metadata(filePath).size ?: error("Empty zip: $zipPath")
    require(size >= 22) { "Zip too small" }

    val tailLen = minOf(65_557L, size)
    val tail = ByteArray(tailLen.toInt())
    fs.source(filePath).buffer().use { source ->
        source.skip(size - tailLen)
        source.readFully(tail)
    }

    val eocdRel = findEocdOffset(tail) ?: error("Invalid zip: EOCD not found")
    val cdSize = readInt(tail, eocdRel + 12).toLong() and 0xFFFF_FFFFL
    val cdOffset = readInt(tail, eocdRel + 16).toLong() and 0xFFFF_FFFFL

    val cd = ByteArray(cdSize.toInt())
    fs.source(filePath).buffer().use { source ->
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
        if (name == entryName || name.endsWith(entryName) || name.endsWith("/$entryName")) {
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

private fun localDataOffset(zipPath: String, localHeaderOffset: Long): Long {
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

private fun readStored(zipPath: String, dataOffset: Long, size: Long): ByteArray {
    val buffer = ByteArray(64 * 1024)
    val out = okio.Buffer()
    FileSystem.SYSTEM.source(zipPath.toPath()).buffer().use { source ->
        source.skip(dataOffset)
        var remaining = size
        while (remaining > 0) {
            val toRead = minOf(buffer.size.toLong(), remaining).toInt()
            val n = source.read(buffer, 0, toRead)
            if (n < 0) error("Unexpected EOF in zip")
            out.write(buffer, 0, n)
            remaining -= n
        }
    }
    return out.readByteArray()
}

@OptIn(ExperimentalForeignApi::class)
private fun readDeflated(zipPath: String, dataOffset: Long, compressedSize: Long): ByteArray {
    memScoped {
        val stream = alloc<z_stream>()
        check(inflateInit2(stream.ptr, -15) == Z_OK) { "inflateInit2 failed" }
        try {
            val inBuf = ByteArray(64 * 1024)
            val outBuf = ByteArray(64 * 1024)
            val out = okio.Buffer()
            FileSystem.SYSTEM.source(zipPath.toPath()).buffer().use { source ->
                source.skip(dataOffset)
                var remaining = compressedSize
                var finished = false
                while (!finished) {
                    if (stream.avail_in.toInt() == 0) {
                        if (remaining <= 0L) error("Deflate ended early")
                        val toRead = minOf(inBuf.size.toLong(), remaining).toInt()
                        val n = source.read(inBuf, 0, toRead)
                        if (n < 0) error("Unexpected EOF in zip deflate stream")
                        remaining -= n
                        inBuf.usePinned { inPinned ->
                            stream.next_in = inPinned.addressOf(0)
                            stream.avail_in = n.toUInt()
                            while (stream.avail_in.toInt() > 0 && !finished) {
                                outBuf.usePinned { outPinned ->
                                    stream.next_out = outPinned.addressOf(0)
                                    stream.avail_out = outBuf.size.toUInt()
                                    val code = inflate(stream.ptr, Z_NO_FLUSH)
                                    val produced = outBuf.size - stream.avail_out.toInt()
                                    if (produced > 0) out.write(outBuf, 0, produced)
                                    when (code) {
                                        Z_OK -> Unit
                                        Z_STREAM_END -> finished = true
                                        else -> error("inflate failed: $code")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return out.readByteArray()
        } finally {
            inflateEnd(stream.ptr)
        }
    }
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
