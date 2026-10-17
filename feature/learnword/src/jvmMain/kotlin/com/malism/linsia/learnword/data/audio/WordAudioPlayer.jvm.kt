package com.malism.linsia.learnword.data.audio

import co.touchlab.kermit.Logger

actual class WordAudioPlayer actual constructor() {
    private val log = Logger.withTag("Audio")

    actual suspend fun play(url: String) {
        log.i { "Desktop audio skipped: $url" }
    }

    actual fun release() = Unit
}
