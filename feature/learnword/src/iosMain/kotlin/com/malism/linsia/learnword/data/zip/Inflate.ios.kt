package com.malism.linsia.learnword.data.zip

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import platform.zlib.Z_FINISH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2
import platform.zlib.z_stream

@OptIn(ExperimentalForeignApi::class)
internal actual fun inflateRawDeflate(input: ByteArray): ByteArray = memScoped {
    val stream = alloc<z_stream>()
    val init = inflateInit2(stream.ptr, -15)
    check(init == Z_OK) { "inflateInit2 failed: $init" }
    val outBuf = ByteArray(input.size * 4 + 64 * 1024)
    val result = input.usePinned { inPinned ->
        outBuf.usePinned { outPinned ->
            stream.next_in = inPinned.addressOf(0)
            stream.avail_in = input.size.toUInt()
            stream.next_out = outPinned.addressOf(0)
            stream.avail_out = outBuf.size.toUInt()
            val code = inflate(stream.ptr, Z_FINISH)
            inflateEnd(stream.ptr)
            check(code == Z_STREAM_END || code == Z_OK) { "inflate failed: $code" }
            val produced = outBuf.size - stream.avail_out.toInt()
            outBuf.copyOf(produced)
        }
    }
    result
}
