package com.malism.linsia.classicread.data.epub

expect fun classicEpubPath(bookId: String): String

fun epubFileName(bookId: String): String = bookId.replace('/', '_') + ".epub"
