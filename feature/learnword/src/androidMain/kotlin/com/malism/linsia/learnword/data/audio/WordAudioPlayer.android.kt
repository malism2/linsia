package com.malism.linsia.learnword.data.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Handler
import android.os.Looper
import co.touchlab.kermit.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class WordAudioPlayer actual constructor() {
    private val log = Logger.withTag("Audio")
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var round = 0
    private val speeds = floatArrayOf(SlowPlaybackSpeed, SlowPlaybackSpeed, NormalPlaybackSpeed)

    actual suspend fun play(url: String) {
        release()
        suspendCancellableCoroutine { cont ->
            try {
                round = 0
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build(),
                    )
                    setDataSource(url)
                    setOnPreparedListener { startRound(it) }
                    setOnCompletionListener {
                        round++
                        if (round < speeds.size) {
                            handler.postDelayed({ startRound(it) }, RepeatGapMs)
                        } else if (cont.isActive) {
                            cont.resume(Unit)
                        }
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

    private fun startRound(mp: MediaPlayer) {
        try {
            mp.playbackParams = PlaybackParams().setSpeed(speeds[round]).setPitch(1f)
            mp.seekTo(0)
            mp.start()
        } catch (t: Throwable) {
            log.w { "start round failed: ${t.message}" }
        }
    }

    actual fun release() {
        handler.removeCallbacksAndMessages(null)
        try {
            player?.release()
        } catch (_: Throwable) {
        }
        player = null
    }
}
