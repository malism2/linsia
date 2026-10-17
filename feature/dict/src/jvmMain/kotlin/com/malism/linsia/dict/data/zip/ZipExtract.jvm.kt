package com.malism.linsia.dict.data.zip

import com.malism.linsia.dict.data.fs.ensureParentDir
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

actual fun extractZipEntryToFile(
    zipPath: String,
    entryName: String,
    destPath: String,
) {
    ensureParentDir(destPath)
    ZipFile(File(zipPath)).use { zip ->
        val entry = zip.getEntry(entryName)
            ?: zip.entries().asSequence().firstOrNull { !it.isDirectory && it.name.endsWith(entryName) }
            ?: error("Zip missing $entryName")
        zip.getInputStream(entry).use { input ->
            FileOutputStream(File(destPath)).use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    if (n > 0) output.write(buffer, 0, n)
                }
                output.flush()
            }
        }
    }
}
