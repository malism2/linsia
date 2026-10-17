package com.malism.linsia.dict.data.fs

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/** Absolute path for the on-disk ECDICT SQLite file. */
expect fun dictDatabasePath(): String

fun dictZipTempPath(): String = dictDatabasePath() + ".zip.download"

fun dictDbStagingPath(): String = dictDatabasePath() + ".staging"

fun dictDatabaseExists(): Boolean = FileSystem.SYSTEM.exists(dictDatabasePath().toPath())

fun deleteDictDatabase() = deleteIfExists(dictDatabasePath())

fun deleteIfExists(path: String) {
    val p = path.toPath()
    if (FileSystem.SYSTEM.exists(p)) {
        FileSystem.SYSTEM.delete(p)
    }
}

fun ensureParentDir(path: String) {
    path.toPath().parent?.let { parent ->
        FileSystem.SYSTEM.createDirectories(parent)
    }
}

/** Test helper / small writes only — avoid for the full ECDICT payload. */
fun writeDictDatabaseBytes(bytes: ByteArray) {
    ensureParentDir(dictDatabasePath())
    FileSystem.SYSTEM.write(dictDatabasePath().toPath()) {
        write(bytes)
    }
}

fun replaceFile(fromPath: String, toPath: String) {
    ensureParentDir(toPath)
    val fs = FileSystem.SYSTEM
    val from = fromPath.toPath()
    val to = toPath.toPath()
    deleteIfExists(toPath)
    fs.atomicMove(from, to)
}

fun readFilePrefix(path: String, maxBytes: Int): ByteArray {
    FileSystem.SYSTEM.source(path.toPath()).buffer().use { source ->
        return source.readByteArray(maxBytes.toLong())
    }
}
