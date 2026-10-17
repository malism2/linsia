package com.malism.linsia.learnword.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

object Sm2Scheduler {
    const val MinEase = 1.3
    const val DefaultEase = 2.5

    fun onGraduateSuccess(state: Sm2State, quality: Int = 4): Pair<Sm2State, LocalDate.(LocalDate) -> LocalDate> {
        val q = quality.coerceIn(0, 5)
        val next = applySuccess(state, q)
        return next to { today -> today.plus(DatePeriod(days = next.intervalDays.coerceAtLeast(1))) }
    }

    fun nextReviewDate(today: LocalDate, intervalDays: Int): LocalDate =
        today.plus(DatePeriod(days = intervalDays.coerceAtLeast(1)))

    fun applySuccess(state: Sm2State, quality: Int = 4): Sm2State {
        val q = quality.coerceIn(3, 5)
        val repetitions = state.repetitions + 1
        val interval = when {
            repetitions == 1 -> 1
            repetitions == 2 -> 6
            else -> (state.intervalDays * state.easeFactor).toInt().coerceAtLeast(1)
        }
        val delta = 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)
        val ease = (state.easeFactor + delta).coerceAtLeast(MinEase)
        return Sm2State(easeFactor = ease, intervalDays = interval, repetitions = repetitions)
    }
}
