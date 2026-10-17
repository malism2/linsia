package com.malism.linsia.learnword.data.audio

import co.touchlab.kermit.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import kotlin.coroutines.resume

actual class WordAudioPlayer actual constructor() {
    private val log = Logger.withTag("Audio")
    private var player: AVPlayer? = null
    private var endObserver: Any? = null
    private var round = 0
    private val speeds = doubleArrayOf(SlowPlaybackSpeed.toDouble(), SlowPlaybackSpeed.toDouble(), NormalPlaybackSpeed.toDouble())

    actual suspend fun play(url: String) {
        release()
        suspendCancellableCoroutine { cont ->
            val nsUrl = NSURL.URLWithString(url)
            if (nsUrl == null) {
                cont.resume(Unit)
                return@suspendCancellableCoroutine
            }
            try {
                round = 0
                val item = AVPlayerItem(uRL = nsUrl)
                val av = AVPlayer(playerItem = item)
                player = av
                endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                    name = AVPlayerItemDidPlayToEndTimeNotification,
                    `object` = item,
                    queue = null,
                ) { _ ->
                    round++
                    if (round < speeds.size) {
                        val delayNs = RepeatGapMs * 1_000_000L
                        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, delayNs), dispatch_get_main_queue()) {
                            startRound(av)
                        }
                    } else if (cont.isActive) {
                        cont.resume(Unit)
                    }
                }
                startRound(av)
                cont.invokeOnCancellation { release() }
            } catch (t: Throwable) {
                log.w { "play failed: ${t.message}" }
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    private fun startRound(av: AVPlayer) {
        val speed = speeds.getOrElse(round) { NormalPlaybackSpeed.toDouble() }
        if (round == 0) {
            av.play()
            av.rate = speed
            return
        }
        av.seekToTime(CMTimeMakeWithSeconds(0.0, 600)) { _ ->
            av.rate = speed
        }
    }

    actual fun release() {
        endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
        player?.pause()
        player = null
    }
}
