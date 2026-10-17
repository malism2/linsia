package com.malism.linsia.dailylisten.data.fs

import java.io.File

actual fun dailyListenAudioPath(lessonId: String): String {
    val dir = File(System.getProperty("user.home"), ".linsia/dailylisten")
    dir.mkdirs()
    return File(dir, audioFileName(lessonId)).absolutePath
}
