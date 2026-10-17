package com.malism.linsia.dailylisten.data

import com.russhwolf.settings.Settings

class DailyListenSettings(private val settings: Settings) {
    var playbackSpeed: Float
        get() = settings.getFloat(SPEED, 1.0f)
        set(value) = settings.putFloat(SPEED, value)

    var showTranscript: Boolean
        get() = settings.getBoolean(TRANSCRIPT, false)
        set(value) = settings.putBoolean(TRANSCRIPT, value)

    private companion object {
        const val SPEED = "dailylisten.playbackSpeed"
        const val TRANSCRIPT = "dailylisten.showTranscript"
    }
}
