package com.rave.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DayPeriodTest {

    @Test
    fun `early hours before 5am count as evening`() {
        assertEquals(DayPeriod.EVENING, dayPeriodFor(0))
        assertEquals(DayPeriod.EVENING, dayPeriodFor(4))
    }

    @Test
    fun `morning runs from 5 to 11`() {
        assertEquals(DayPeriod.MORNING, dayPeriodFor(5))
        assertEquals(DayPeriod.MORNING, dayPeriodFor(11))
    }

    @Test
    fun `afternoon runs from 12 to 16`() {
        assertEquals(DayPeriod.AFTERNOON, dayPeriodFor(12))
        assertEquals(DayPeriod.AFTERNOON, dayPeriodFor(16))
    }

    @Test
    fun `evening starts at 17`() {
        assertEquals(DayPeriod.EVENING, dayPeriodFor(17))
        assertEquals(DayPeriod.EVENING, dayPeriodFor(23))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects out of range hour`() {
        dayPeriodFor(24)
    }
}
