package com.rave.app.domain.wrapped

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.Month

class WrappedCalculatorTest {
    private fun d(m: Int, day: Int, y: Int = 2026) = LocalDate.of(y, m, day)

    @Test
    fun `only the requested year is counted`() {
        val s = WrappedCalculator.summarize(
            period = WrappedPeriod(2026),
            dailySteps = listOf(d(3, 1) to 10_000, d(3, 2) to 12_000, d(12, 31, 2025) to 50_000),
            activities = listOf(WrappedActivity(d(3, 2), 5200.0, 3600, "Long walk"), WrappedActivity(d(1, 1, 2025), 9000.0, 1, "old")),
            habitsDone = listOf(WrappedHabitDone("Read", d(3, 3)), WrappedHabitDone("Read", d(3, 4)), WrappedHabitDone("Water", d(4, 1))),
            workouts = listOf(WrappedWorkout(d(5, 5), 3200.0)),
            bestStreak = 21,
        )
        assertEquals(22_000, s.totalSteps)
        assertEquals(d(3, 2) to 12_000, s.bestStepDay)
        assertEquals(5.2, s.totalKm, 1e-9)
        assertEquals("Long walk", s.longest?.name)
        assertEquals("Read" to 2, s.topHabit)
        assertEquals(1, s.workouts)
        assertEquals(Month.MARCH, s.peakMonth)
        assertEquals(WrappedPersona.ALL_ROUNDER, s.persona)
    }

    @Test
    fun `empty year is a fresh start`() {
        val s = WrappedCalculator.summarize(WrappedPeriod(2026), emptyList(), emptyList(), emptyList(), emptyList(), 0)
        assertTrue(s.isEmpty)
        assertNull(s.peakMonth)
        assertEquals(WrappedPersona.FRESH_START, s.persona)
    }

    @Test
    fun `monthly wrapped counts only that month and has no peak month`() {
        val s = WrappedCalculator.summarize(
            period = WrappedPeriod(2026, 3),
            dailySteps = listOf(d(3, 1) to 10_000, d(3, 31) to 2_000, d(4, 1) to 50_000),
            activities = listOf(WrappedActivity(d(3, 2), 12_000.0, 3600, "Long walk")),
            habitsDone = emptyList(),
            workouts = emptyList(),
            bestStreak = 3,
        )
        assertEquals(12_000, s.totalSteps)
        assertEquals(3, s.month)
        assertNull(s.peakMonth)
        // 12 km in a month is a road warrior month (100 km a year / 12).
        assertEquals(WrappedPersona.ROAD_WARRIOR, s.persona)
    }

    @Test
    fun `monthly wrapped opens on the last day and stays a week, yearly only around new year`() {
        assertEquals(listOf(WrappedPeriod(2026, 10)), WrappedCalculator.available(d(10, 31)))
        assertEquals(listOf(WrappedPeriod(2026, 9)), WrappedCalculator.available(d(10, 7)))
        assertEquals(emptyList<WrappedPeriod>(), WrappedCalculator.available(d(10, 8)))
        assertEquals(listOf(WrappedPeriod(2026, 2)), WrappedCalculator.available(d(2, 28)))
        assertEquals(listOf(WrappedPeriod(2026), WrappedPeriod(2026, 12)), WrappedCalculator.available(d(12, 31)))
        assertEquals(listOf(WrappedPeriod(2026), WrappedPeriod(2026, 12)), WrappedCalculator.available(d(1, 5, 2027)))
        assertEquals(listOf(WrappedPeriod(2026)), WrappedCalculator.available(d(1, 10, 2027)))
        assertEquals(emptyList<WrappedPeriod>(), WrappedCalculator.available(d(1, 20, 2027)))
        assertEquals(emptyList<WrappedPeriod>(), WrappedCalculator.available(d(12, 5)))
    }
}
