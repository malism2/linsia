package com.malism.linsia.typeword.data.audio

expect class WordAudioPlayer() {
    suspend fun play(url: String)
    fun release()
}

object VoiceUrl {
    fun youdao(word: String, uk: Boolean): String {
        val type = if (uk) 1 else 2
        val query = word.trim()
        return "https://dict.youdao.com/dictvoice?audio=$query&type=$type"
    }
}
