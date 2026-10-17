package com.malism.linsia.dailylisten.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

object ListenStreak {
    /**
     * Counts consecutive completed calendar days ending at [today].
     * If today is not completed, counts the streak ending yesterday.
     */
    fun compute(completedDates: Set<String>, today: LocalDate): Int {
        var cursor = today
        if (today.toString() !in completedDates) {
            cursor = today.minus(DatePeriod(days = 1))
        }
        var count = 0
        while (cursor.toString() in completedDates) {
            count++
            cursor = cursor.minus(DatePeriod(days = 1))
        }
        return count
    }
}
