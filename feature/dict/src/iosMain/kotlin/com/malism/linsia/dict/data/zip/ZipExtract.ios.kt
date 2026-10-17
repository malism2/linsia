package com.malism.linsia.dict.data.zip

import com.malism.linsia.dict.data.fs.ensureParentDir
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
actual fun extractZipEntryToFile(
    zipPath: String,
    entryName: String,
    destPath: String,
) {
    ensureParentDir(destPath)
    val entry = locateZipEntry(zipPath, entryName)
    val dataOffset = localDataOffset(zipPath, entry.localHeaderOffset)
    val dest = destPath.toPath()
    if (FileSystem.SYSTEM.exists(dest)) {
        FileSystem.SYSTEM.delete(dest)
    }

    when (entry.method) {
        0 -> copyStored(zipPath, dataOffset, entry.compressedSize, destPath)
        8 -> inflateEntry(zipPath, dataOffset, entry.compressedSize, destPath)
        else -> error("Unsupported zip method ${entry.method}")
    }
    check(FileSystem.SYSTEM.exists(dest)) { "Extract produced no file" }
}

private fun copyStored(zipPath: String, dataOffset: Long, size: Long, destPath: String) {
    val buffer = ByteArray(64 * 1024)
    FileSystem.SYSTEM.source(zipPath.toPath()).buffer().use { source ->
        source.skip(dataOffset)
        FileSystem.SYSTEM.sink(destPath.toPath()).buffer().use { sink ->
            var remaining = size
            while (remaining > 0) {
                val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                val n = source.read(buffer, 0, toRead)
                if (n < 0) error("Unexpected EOF in zip")
                sink.write(buffer, 0, n)
                remaining -= n
            }
            sink.flush()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun inflateEntry(zipPath: String, dataOffset: Long, compressedSize: Long, destPath: String) {
    memScoped {
        val stream = alloc<z_stream>()
        check(inflateInit2(stream.ptr, -15) == Z_OK) { "inflateInit2 failed" }
        try {
            val inBuf = ByteArray(64 * 1024)
            val outBuf = ByteArray(64 * 1024)
            FileSystem.SYSTEM.source(zipPath.toPath()).buffer().use { source ->
                source.skip(dataOffset)
                FileSystem.SYSTEM.sink(destPath.toPath()).buffer().use { sink ->
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
                                        if (produced > 0) sink.write(outBuf, 0, produced)
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
                    sink.flush()
                }
            }
        } finally {
            inflateEnd(stream.ptr)
        }
    }
}
