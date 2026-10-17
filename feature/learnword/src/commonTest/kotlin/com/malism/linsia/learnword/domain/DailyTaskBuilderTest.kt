package com.malism.linsia.learnword.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DailyTaskBuilderTest {
    @Test
    fun nEqualsXPlusYPlusZ() {
        val input = TaskInput(
            newWords = (1L..10L).map { TaskWord(it, it.toInt(), WordSource.New) },
            dueReview = (11L..13L).map { TaskWord(it, it.toInt(), WordSource.Review) },
            leftovers = listOf(TaskWord(20, 20, WordSource.Leftover)),
            dailyNewLimit = 5,
        )
        val pack = DailyTaskBuilder.build(input)
        assertEquals(5 + 3 + 1, pack.size)
        assertEquals(5, pack.count { it.source == WordSource.New })
        assertEquals(3, pack.count { it.source == WordSource.Review })
        assertEquals(1, pack.count { it.source == WordSource.Leftover })
    }

    @Test
    fun newWordsFollowOrdinalAndCap() {
        val input = TaskInput(
            newWords = listOf(
                TaskWord(3, 30, WordSource.New),
                TaskWord(1, 10, WordSource.New),
                TaskWord(2, 20, WordSource.New),
            ),
            dueReview = emptyList(),
            leftovers = emptyList(),
            dailyNewLimit = 2,
        )
        val pack = DailyTaskBuilder.build(input)
        assertEquals(listOf(1L, 2L), pack.map { it.wordId })
    }

    @Test
    fun leftoversDedupedAgainstReview() {
        val input = TaskInput(
            newWords = emptyList(),
            dueReview = listOf(TaskWord(1, 1, WordSource.Review)),
            leftovers = listOf(TaskWord(1, 1, WordSource.Leftover), TaskWord(2, 2, WordSource.Leftover)),
            dailyNewLimit = 20,
        )
        val pack = DailyTaskBuilder.build(input)
        assertEquals(2, pack.size)
        assertEquals(1, pack.count { it.wordId == 1L })
    }

    @Test
    fun interleavePutsReviewFirst() {
        val review = (1L..4L).map { TaskWord(it, it.toInt(), WordSource.Review) }
        val news = (10L..13L).map { TaskWord(it, it.toInt(), WordSource.New) }
        val pack = DailyTaskBuilder.interleave(review, news)
        assertTrue(pack.take(2).all { it.source == WordSource.Review })
        assertTrue(pack.map { it.wordId }.toSet().size == 8)
    }
}
