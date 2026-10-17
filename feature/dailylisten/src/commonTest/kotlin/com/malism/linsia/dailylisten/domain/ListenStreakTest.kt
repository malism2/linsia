package com.malism.linsia.dailylisten.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ListenStreakTest {
    @Test
    fun countsIncludingToday() {
        val dates = setOf("2026-09-04", "2026-09-03", "2026-09-02")
        assertEquals(3, ListenStreak.compute(dates, LocalDate(2026, 9, 4)))
    }

    @Test
    fun fallsBackToYesterdayWhenTodayMissing() {
        val dates = setOf("2026-09-03", "2026-09-02")
        assertEquals(2, ListenStreak.compute(dates, LocalDate(2026, 9, 4)))
    }

    @Test
    fun breaksOnGap() {
        val dates = setOf("2026-09-04", "2026-09-02")
        assertEquals(1, ListenStreak.compute(dates, LocalDate(2026, 9, 4)))
    }

    @Test
    fun emptyIsZero() {
        assertEquals(0, ListenStreak.compute(emptySet(), LocalDate(2026, 9, 4)))
    }
}
