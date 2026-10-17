package com.malism.linsia.typeword.data.audio

import co.touchlab.kermit.Logger

actual class WordAudioPlayer actual constructor() {
    private val log = Logger.withTag("TypewordAudio")

    actual suspend fun play(url: String) {
        log.i { "Desktop audio skipped: $url" }
    }

    actual fun release() = Unit
}
