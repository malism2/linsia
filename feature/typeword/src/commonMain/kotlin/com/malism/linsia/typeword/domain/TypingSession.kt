package com.malism.linsia.typeword.domain

class TypingSession(
    words: List<DictWord>,
    val dictation: Boolean = false,
    private val nowMs: () -> Long,
) {
    private val words: List<DictWord> = words.map { it.copy(name = it.name.lowercase()) }
    private val startedAt = nowMs()
    private var index = 0
    private var typed = StringBuilder()
    private var wrongChar: Char? = null
    private var correctChars = 0
    private var errorCount = 0
    private var skipped = 0
    private var finished = false

    init {
        if (this.words.isEmpty()) finished = true
    }

    fun snapshot(): TypingSnapshot {
        val elapsed = (nowMs() - startedAt).coerceAtLeast(0)
        val word = when {
            finished && index > 0 -> words.getOrNull(index - 1)
            else -> currentWord()
        }
        return TypingSnapshot(
            word = word,
            typed = typed.toString(),
            wrongChar = wrongChar,
            wordIndex = index.coerceAtMost(words.size),
            wordCount = words.size,
            correctChars = correctChars,
            errorCount = errorCount,
            skipped = skipped,
            elapsedMs = elapsed,
            dictation = dictation,
            finished = finished,
        )
    }

    fun stats(): ChapterStats = ChapterStats(
        wordCount = words.size,
        correctChars = correctChars,
        errorCount = errorCount,
        skipped = skipped,
        elapsedMs = (nowMs() - startedAt).coerceAtLeast(0),
    )

    fun clearFlash() {
        wrongChar = null
    }

    fun type(raw: Char): TypingResult {
        if (finished) return TypingResult.Continue
        val word = currentWord() ?: return TypingResult.Continue
        val ch = raw.lowercaseChar()
        if (!isTypeable(ch)) return TypingResult.Continue
        val expected = word.name.getOrNull(typed.length) ?: return TypingResult.Continue
        return if (ch == expected) {
            typed.append(ch)
            correctChars++
            wrongChar = null
            if (typed.length >= word.name.length) finishCurrentWord() else TypingResult.Continue
        } else {
            errorCount++
            wrongChar = ch
            TypingResult.Continue
        }
    }

    fun skip(): TypingResult {
        if (finished) return TypingResult.Continue
        val word = currentWord() ?: return TypingResult.Continue
        skipped++
        return finishCurrentWord(completedWord = word)
    }

    private fun finishCurrentWord(completedWord: DictWord? = currentWord()): TypingResult {
        val word = completedWord ?: return TypingResult.Continue
        val done = index + 1 >= words.size
        index++
        wrongChar = null
        if (done) {
            finished = true
            typed = StringBuilder(word.name)
            return TypingResult.WordFinished(word, chapterDone = true)
        }
        typed = StringBuilder()
        return TypingResult.WordFinished(word, chapterDone = false)
    }

    private fun currentWord(): DictWord? = words.getOrNull(index)

    companion object {
        fun isTypeable(ch: Char): Boolean =
            ch in 'a'..'z' || ch == '\'' || ch == '-'
    }
}
