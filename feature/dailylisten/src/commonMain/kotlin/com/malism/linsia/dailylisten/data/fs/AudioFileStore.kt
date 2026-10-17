package com.malism.linsia.dailylisten.data.fs

expect fun dailyListenAudioPath(lessonId: String): String

fun audioFileName(lessonId: String): String =
    lessonId.replace('/', '_') + ".mp3"
