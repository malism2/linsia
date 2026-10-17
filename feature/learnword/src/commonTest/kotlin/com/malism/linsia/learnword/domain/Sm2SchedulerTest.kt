package com.malism.linsia.learnword.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Sm2SchedulerTest {
    @Test
    fun firstSuccessIntervalIsOneDay() {
        val next = Sm2Scheduler.applySuccess(Sm2State())
        assertEquals(1, next.intervalDays)
        assertEquals(1, next.repetitions)
        assertTrue(next.easeFactor >= Sm2Scheduler.MinEase)
    }

    @Test
    fun secondSuccessIntervalIsSixDays() {
        val first = Sm2Scheduler.applySuccess(Sm2State())
        val second = Sm2Scheduler.applySuccess(first)
        assertEquals(6, second.intervalDays)
        assertEquals(2, second.repetitions)
    }

    @Test
    fun laterIntervalMultipliesEase() {
        var state = Sm2State()
        repeat(3) { state = Sm2Scheduler.applySuccess(state) }
        assertTrue(state.intervalDays >= 6)
        val today = LocalDate(2026, 8, 13)
        val nextDate = Sm2Scheduler.nextReviewDate(today, state.intervalDays)
        assertEquals(today.toEpochDays() + state.intervalDays, nextDate.toEpochDays())
    }

    @Test
    fun easeNeverBelowMinimum() {
        var state = Sm2State(easeFactor = 1.3)
        state = Sm2Scheduler.applySuccess(state, quality = 3)
        assertTrue(state.easeFactor >= 1.3)
    }
}
