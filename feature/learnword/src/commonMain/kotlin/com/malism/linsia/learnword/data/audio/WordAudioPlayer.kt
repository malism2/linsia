package com.malism.linsia.learnword.data.audio

internal const val SlowPlaybackSpeed = 0.7f
internal const val NormalPlaybackSpeed = 1f
internal const val RepeatGapMs = 200L

expect class WordAudioPlayer() {
    /** Plays twice slowly, then once at normal speed. */
    suspend fun play(url: String)
    fun release()
}

object VoiceUrl {
    fun youdao(word: String, uk: Boolean): String {
        val type = if (uk) 1 else 2
        return "https://dict.youdao.com/dictvoice?audio=$word&type=$type"
    }
}
