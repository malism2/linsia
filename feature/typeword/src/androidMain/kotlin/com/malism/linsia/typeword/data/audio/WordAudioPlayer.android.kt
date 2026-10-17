package com.malism.linsia.typeword.data.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import co.touchlab.kermit.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class WordAudioPlayer actual constructor() {
    private val log = Logger.withTag("TypewordAudio")
    private var player: MediaPlayer? = null

    actual suspend fun play(url: String) {
        release()
        suspendCancellableCoroutine { cont ->
            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build(),
                    )
                    setDataSource(url)
                    setOnPreparedListener { start() }
                    setOnCompletionListener {
                        if (cont.isActive) cont.resume(Unit)
                    }
                    setOnErrorListener { _, _, _ ->
                        if (cont.isActive) cont.resume(Unit)
                        true
                    }
                    prepareAsync()
                }
                player = mp
                cont.invokeOnCancellation { release() }
            } catch (t: Throwable) {
                log.w { "play failed: ${t.message}" }
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    actual fun release() {
        try {
            player?.release()
        } catch (_: Throwable) {
        }
        player = null
    }
}
