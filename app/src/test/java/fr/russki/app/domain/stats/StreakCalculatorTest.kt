package fr.russki.app.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val today = LocalDate.of(2026, 11, 18)

    @Test
    fun emptyHistory_zeroStreaks() {
        assertEquals(0, StreakCalculator.currentStreak(emptySet(), today))
        assertEquals(0, StreakCalculator.bestStreak(emptySet()))
        assertFalse(StreakCalculator.isActiveOn(emptySet(), today))
    }

    @Test
    fun consecutiveDays_includingToday() {
        val days = setOf("2026-11-16", "2026-11-17", "2026-11-18")
        assertEquals(3, StreakCalculator.currentStreak(days, today))
        assertEquals(3, StreakCalculator.bestStreak(days))
        assertTrue(StreakCalculator.isActiveOn(days, today))
    }

    @Test
    fun streakContinuesFromYesterday_ifNoActivityTodayYet() {
        val days = setOf("2026-11-16", "2026-11-17")
        assertEquals(2, StreakCalculator.currentStreak(days, today))
        assertFalse(StreakCalculator.isActiveOn(days, today))
    }

    @Test
    fun gapBreaksCurrentStreak_butKeepsBest() {
        val days = setOf("2026-11-10", "2026-11-11", "2026-11-12", "2026-11-18")
        assertEquals(1, StreakCalculator.currentStreak(days, today))
        assertEquals(3, StreakCalculator.bestStreak(days))
    }

    @Test
    fun missedDay_resetsCurrentToZeroIfOlderThanYesterday() {
        val days = setOf("2026-11-10", "2026-11-11")
        assertEquals(0, StreakCalculator.currentStreak(days, today))
        assertEquals(2, StreakCalculator.bestStreak(days))
    }

    @Test
    fun multipleEventsSameDay_countOnceInSet() {
        val days = setOf("2026-11-18")
        assertEquals(1, StreakCalculator.currentStreak(days, today))
        assertEquals(1, days.size)
    }
}
