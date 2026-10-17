package com.malism.linsia.dailylisten.data.fs

import com.malism.linsia.core.platform.AndroidAppContext
import java.io.File

actual fun dailyListenAudioPath(lessonId: String): String {
    val dir = File(AndroidAppContext.require().filesDir, "dailylisten")
    dir.mkdirs()
    return File(dir, audioFileName(lessonId)).absolutePath
}
