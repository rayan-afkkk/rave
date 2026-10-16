package com.rave.app.domain.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

class NotifyTest {
    private val karachi = ZoneId.of("Asia/Karachi")

    private fun near(expected: LocalTime, actual: LocalTime?, minutes: Int = 4) {
        assertNotNull(actual)
        val diff = abs(expected.toSecondOfDay() - actual!!.toSecondOfDay()) / 60
        assertTrue("expected ~$expected but was $actual", diff <= minutes)
    }

    @Test
    fun `lahore sunset summer and winter`() {
        near(LocalTime.of(19, 9), SunTimes.sunset(LocalDate.of(2026, 6, 21), 31.5204, 74.3587, karachi))
        near(LocalTime.of(17, 3), SunTimes.sunset(LocalDate.of(2026, 12, 21), 31.5204, 74.3587, karachi))
    }

    @Test
    fun `london sunset uses daylight saving`() {
        near(LocalTime.of(21, 21), SunTimes.sunset(LocalDate.of(2026, 6, 21), 51.5074, -0.1278, ZoneId.of("Europe/London")))
    }

    @Test
    fun `no sunset in polar summer`() {
        assertNull(SunTimes.sunset(LocalDate.of(2026, 6, 21), 78.22, 15.65, ZoneId.of("Arctic/Longyearbyen")))
    }

    @Test
    fun `nudge rounds the walk to 5 minutes and skips when close`() {
        assertNull(StepNudges.evaluate(7_900, 8_000))
        assertNull(StepNudges.evaluate(9_000, 8_000))
        val n = StepNudges.evaluate(6_000, 8_000)!!
        assertEquals(2_000, n.stepsShort)
        assertEquals(20, n.walkMinutes)
        assertTrue(n.closesGap)
        val far = StepNudges.evaluate(500, 8_000)!!
        assertEquals(45, far.walkMinutes)
        assertTrue(!far.closesGap)
    }

    @Test
    fun `nudge time follows maghrib with fallback and cap`() {
        assertEquals(LocalTime.of(19, 29), StepNudges.nudgeTime(LocalTime.of(19, 9)))
        assertEquals(LocalTime.of(19, 30), StepNudges.nudgeTime(null))
        assertEquals(LocalTime.of(21, 30), StepNudges.nudgeTime(LocalTime.of(21, 25)))
    }

    @Test
    fun `weekly change percent`() {
        assertEquals(12, WeeklyReports.changePercent(33.6, 30.0))
        assertEquals(-50, WeeklyReports.changePercent(5.0, 10.0))
        assertNull(WeeklyReports.changePercent(5.0, 0.0))
        val r = WeeklyReport(LocalDate.of(2026, 10, 12), 54_000, 50_000, 32_000.0, 28_500.0, 4, 18, 21, 11_000, 12)
        assertEquals(12, r.distanceChange)
        assertEquals(8, r.stepsChange)
        assertEquals(86, r.habitRate)
        assertTrue(r.leadsWithDistance)
    }
}
