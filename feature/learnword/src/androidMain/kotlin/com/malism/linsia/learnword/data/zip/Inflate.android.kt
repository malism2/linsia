package com.malism.linsia.learnword.data.zip

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

internal actual fun inflateRawDeflate(input: ByteArray): ByteArray {
    val inflater = Inflater(true)
    inflater.setInput(input)
    val out = ByteArrayOutputStream()
    val buf = ByteArray(16 * 1024)
    while (!inflater.finished()) {
        val n = inflater.inflate(buf)
        if (n == 0 && inflater.needsInput()) break
        out.write(buf, 0, n)
    }
    inflater.end()
    return out.toByteArray()
}
