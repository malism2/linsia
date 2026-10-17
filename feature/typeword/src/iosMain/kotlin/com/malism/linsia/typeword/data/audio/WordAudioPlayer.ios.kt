package com.malism.linsia.typeword.data.audio

import co.touchlab.kermit.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.play
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import kotlin.coroutines.resume

actual class WordAudioPlayer actual constructor() {
    private val log = Logger.withTag("TypewordAudio")
    private var player: AVPlayer? = null
    private var endObserver: Any? = null

    actual suspend fun play(url: String) {
        release()
        suspendCancellableCoroutine { cont ->
            val nsUrl = NSURL.URLWithString(url)
            if (nsUrl == null) {
                cont.resume(Unit)
                return@suspendCancellableCoroutine
            }
            try {
                val item = AVPlayerItem(uRL = nsUrl)
                val av = AVPlayer(playerItem = item)
                player = av
                endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                    name = AVPlayerItemDidPlayToEndTimeNotification,
                    `object` = item,
                    queue = null,
                ) { _ ->
                    if (cont.isActive) cont.resume(Unit)
                }
                av.play()
                cont.invokeOnCancellation { release() }
            } catch (t: Throwable) {
                log.w { "play failed: ${t.message}" }
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    actual fun release() {
        endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
        player?.pause()
        player = null
    }
}
