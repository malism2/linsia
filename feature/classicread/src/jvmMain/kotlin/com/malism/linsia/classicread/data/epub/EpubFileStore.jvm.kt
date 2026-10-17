package com.malism.linsia.classicread.data.epub

import java.io.File

actual fun classicEpubPath(bookId: String): String {
    val dir = File(System.getProperty("user.home"), ".linsia/epubs")
    dir.mkdirs()
    return File(dir, epubFileName(bookId)).absolutePath
}
