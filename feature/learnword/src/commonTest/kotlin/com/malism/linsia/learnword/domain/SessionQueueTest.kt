package com.malism.linsia.learnword.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionQueueTest {
    private fun cards(vararg ids: Long) = ids.map {
        SessionCard(wordId = it, life = 2, source = WordSource.New)
    }

    @Test
    fun knowDecrementsLifeAndReinserts() {
        val queue = SessionQueue(cards(1, 2, 3, 4, 5, 6, 7, 8, 9), insertOffset = { 5 })
        val result = queue.know()
        assertNull(result.graduated)
        val remainingIds = queue.remaining.map { it.wordId }
        assertEquals(1L, remainingIds[5])
        assertEquals(1, queue.remaining.first { it.wordId == 1L }.life)
    }

    @Test
    fun twoKnowsGraduate() {
        val queue = SessionQueue(cards(1), insertOffset = { 5 })
        assertNull(queue.know().graduated)
        val second = queue.know()
        assertEquals(1L, second.graduated?.wordId)
        assertTrue(queue.isEmpty)
    }

    @Test
    fun unknownReinsertsWithoutGraduating() {
        val queue = SessionQueue(cards(1, 2, 3), insertOffset = { 5 })
        queue.markUnknownAndReinsert()
        val ids = queue.remaining.map { it.wordId }
        assertTrue(1L in ids)
        assertEquals(2, queue.remaining.first { it.wordId == 1L }.life)
        assertEquals(false, queue.remaining.first { it.wordId == 1L }.firstPassKnown)
    }

    @Test
    fun shortQueueInsertsAtTail() {
        val queue = SessionQueue(cards(1, 2), insertOffset = { 8 })
        queue.markUnknownAndReinsert()
        assertEquals(listOf(2L, 1L), queue.remaining.map { it.wordId })
    }
}
