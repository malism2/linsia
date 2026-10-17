package com.malism.linsia.classicread.data.epub

import com.malism.linsia.core.platform.AndroidAppContext
import java.io.File

actual fun classicEpubPath(bookId: String): String {
    val dir = File(AndroidAppContext.require().filesDir, "epubs")
    dir.mkdirs()
    return File(dir, epubFileName(bookId)).absolutePath
}
