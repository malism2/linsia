package com.malism.linsia.typeword.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TypingSessionTest {
    private val words = listOf(
        DictWord("cat", listOf("猫"), "kæt", "kæt"),
        DictWord("don't", listOf("不要"), "doʊnt", "dəʊnt"),
        DictWord("well-known", listOf("著名的"), "wel noʊn", "wel nəʊn"),
    )

    @Test
    fun correctLettersAdvance() {
        val session = TypingSession(words.take(1), nowMs = { 0 })
        assertEquals(TypingResult.Continue, session.type('c'))
        assertEquals(TypingResult.Continue, session.type('a'))
        val done = session.type('t')
        assertEquals(TypingResult.WordFinished(words[0].copy(name = "cat"), chapterDone = true), done)
        assertTrue(session.snapshot().finished)
        assertEquals(3, session.snapshot().correctChars)
        assertEquals(0, session.snapshot().errorCount)
    }

    @Test
    fun wrongKeyDoesNotAdvance() {
        val session = TypingSession(words.take(1), nowMs = { 0 })
        session.type('c')
        val result = session.type('x')
        assertEquals(TypingResult.Continue, result)
        val snap = session.snapshot()
        assertEquals("c", snap.typed)
        assertEquals('x', snap.wrongChar)
        assertEquals(1, snap.errorCount)
        assertEquals(1, snap.correctChars)
    }

    @Test
    fun mustTypeCorrectLetterAfterError() {
        val session = TypingSession(words.take(1), nowMs = { 0 })
        session.type('x')
        assertEquals("", session.snapshot().typed)
        session.type('c')
        assertEquals("c", session.snapshot().typed)
        assertNull(session.snapshot().wrongChar)
    }

    @Test
    fun caseInsensitiveAndFiltersSymbols() {
        val session = TypingSession(words.take(1), nowMs = { 0 })
        session.type('C')
        session.type('!')
        session.type('A')
        val done = session.type('T')
        assertTrue(done is TypingResult.WordFinished)
        assertEquals(3, session.snapshot().correctChars)
        assertEquals(0, session.snapshot().errorCount)
    }

    @Test
    fun apostropheAndHyphen() {
        val session = TypingSession(listOf(words[1], words[2]), nowMs = { 0 })
        "don't".forEach { session.type(it) }
        val first = session.snapshot()
        assertEquals(1, first.wordIndex)
        assertEquals("well-known", first.word?.name)
        "well-known".forEach { session.type(it) }
        assertTrue(session.snapshot().finished)
    }

    @Test
    fun skipAdvancesAndCounts() {
        val session = TypingSession(words.take(2), nowMs = { 0 })
        val skipped = session.skip()
        assertEquals(TypingResult.WordFinished(words[0].copy(name = "cat"), chapterDone = false), skipped)
        assertEquals(1, session.snapshot().skipped)
        assertEquals("don't", session.snapshot().word?.name)
    }

    @Test
    fun skipLastWordFinishesChapter() {
        val session = TypingSession(words.take(1), nowMs = { 0 })
        val result = session.skip()
        assertEquals(TypingResult.WordFinished(words[0].copy(name = "cat"), chapterDone = true), result)
        assertTrue(session.snapshot().finished)
    }

    @Test
    fun dictationFlagIsVisibleOnSnapshot() {
        val session = TypingSession(words.take(1), dictation = true, nowMs = { 0 })
        assertTrue(session.snapshot().dictation)
        assertEquals("猫", session.snapshot().translation)
    }

    @Test
    fun wpmAndAccuracy() {
        var now = 0L
        val session = TypingSession(words.take(1), nowMs = { now })
        "cat".forEach { session.type(it) }
        now = 60_000
        val stats = session.stats()
        assertEquals(3, stats.correctChars)
        assertEquals(0.0, stats.accuracy - 1.0, 1e-9)
        assertEquals(3 / 5.0, stats.wpm, 1e-9)
    }

    @Test
    fun accuracyCountsErrors() {
        val session = TypingSession(words.take(1), nowMs = { 60_000 })
        session.type('x')
        session.type('y')
        "cat".forEach { session.type(it) }
        val stats = session.stats()
        assertEquals(3, stats.correctChars)
        assertEquals(2, stats.errorCount)
        assertEquals(3.0 / 5.0, stats.accuracy, 1e-9)
    }

    @Test
    fun chapterSlice() {
        val list = (1..45).map { DictWord("w$it", listOf("$it")) }
        assertEquals(3, Chapters.count(45))
        assertEquals(20, Chapters.slice(list, 0).size)
        assertEquals(20, Chapters.slice(list, 1).size)
        assertEquals(5, Chapters.slice(list, 2).size)
        assertEquals("w21", Chapters.slice(list, 1).first().name)
    }

    @Test
    fun emptySessionIsFinished() {
        val session = TypingSession(emptyList(), nowMs = { 0 })
        assertTrue(session.snapshot().finished)
        assertEquals(TypingResult.Continue, session.type('a'))
        assertFalse(session.snapshot().dictation)
    }
}
