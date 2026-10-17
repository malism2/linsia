package com.malism.linsia.core.io

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

fun ensureParentDir(path: String) {
    path.toPath().parent?.let { parent ->
        FileSystem.SYSTEM.createDirectories(parent)
    }
}

fun deleteIfExists(path: String) {
    val p = path.toPath()
    if (FileSystem.SYSTEM.exists(p)) {
        FileSystem.SYSTEM.delete(p)
    }
}

fun readFilePrefix(path: String, maxBytes: Int): ByteArray {
    FileSystem.SYSTEM.source(path.toPath()).buffer().use { source ->
        return source.readByteArray(maxBytes.toLong())
    }
}

fun requireValidZipFile(path: String, invalidMessage: String = "下载失败（不是 zip，可能是错误页）") {
    val head = readFilePrefix(path, 4)
    val isZip = head.size >= 4 &&
        head[0] == 0x50.toByte() && head[1] == 0x4B.toByte() &&
        (head[2] == 0x03.toByte() || head[2] == 0x05.toByte() || head[2] == 0x07.toByte())
    if (!isZip) {
        val preview = readFilePrefix(path, 120).decodeToString().replace('\n', ' ')
        error("$invalidMessage: $preview")
    }
}
