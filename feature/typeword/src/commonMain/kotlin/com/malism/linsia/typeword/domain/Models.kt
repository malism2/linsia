package com.malism.linsia.typeword.domain

import kotlinx.serialization.Serializable

@Serializable
data class DictWord(
    val name: String,
    val trans: List<String> = emptyList(),
    val usphone: String = "",
    val ukphone: String = "",
)

@Serializable
data class WordDictFile(
    val id: String,
    val title: String,
    val words: List<DictWord> = emptyList(),
)

data class WordDict(
    val id: String,
    val title: String,
    val words: List<DictWord>,
) {
    val chapterCount: Int get() = Chapters.count(words.size)

    fun chapter(index: Int): List<DictWord> = Chapters.slice(words, index)
}

@Serializable
data class ChapterStats(
    val wordCount: Int,
    val correctChars: Int,
    val errorCount: Int,
    val skipped: Int,
    val elapsedMs: Long,
) {
    val accuracy: Double
        get() {
            val total = correctChars + errorCount
            return if (total == 0) 1.0 else correctChars.toDouble() / total
        }

    val wpm: Double
        get() {
            val minutes = elapsedMs / 60_000.0
            return if (minutes <= 0.0) 0.0 else (correctChars / 5.0) / minutes
        }
}

data class TypingSnapshot(
    val word: DictWord?,
    val typed: String,
    val wrongChar: Char?,
    val wordIndex: Int,
    val wordCount: Int,
    val correctChars: Int,
    val errorCount: Int,
    val skipped: Int,
    val elapsedMs: Long,
    val dictation: Boolean,
    val finished: Boolean,
) {
    val accuracy: Double
        get() {
            val total = correctChars + errorCount
            return if (total == 0) 1.0 else correctChars.toDouble() / total
        }

    val wpm: Double
        get() {
            val minutes = elapsedMs / 60_000.0
            return if (minutes <= 0.0) 0.0 else (correctChars / 5.0) / minutes
        }

    val translation: String
        get() = word?.trans?.joinToString("；").orEmpty()
}

sealed interface TypingResult {
    data object Continue : TypingResult
    data class WordFinished(val word: DictWord, val chapterDone: Boolean) : TypingResult
}

object Chapters {
    const val Size = 20

    fun count(wordCount: Int): Int =
        if (wordCount <= 0) 0 else (wordCount + Size - 1) / Size

    fun slice(words: List<DictWord>, index: Int): List<DictWord> {
        if (words.isEmpty()) return emptyList()
        val start = index.coerceIn(0, (count(words.size) - 1).coerceAtLeast(0)) * Size
        return words.drop(start).take(Size)
    }
}

object TypewordDefaults {
    const val DefaultDictId = "cet4"
    val DictFiles = listOf("cet4.json", "cet6.json", "coder.json")
}
