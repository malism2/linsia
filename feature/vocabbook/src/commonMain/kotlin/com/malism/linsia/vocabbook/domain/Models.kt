package com.malism.linsia.vocabbook.domain

object VocabSourceType {
    const val CLASSIC = "classic"
    const val NEWS = "news"
    const val MANUAL = "manual"
}

data class VocabWord(
    val word: String,
    val wordLower: String,
    val phonetic: String?,
    val translation: String,
    val sourceType: String,
    val sourceTitle: String?,
    val createdAt: Long,
)
