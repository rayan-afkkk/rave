package com.rave.app.domain.fitness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FitnessMathTest {

    @Test
    fun `stride and distance follow height`() {
        assertEquals(0.7055, FitnessMath.strideM(170f), 1e-4)
        assertEquals(5644.0, FitnessMath.distanceM(8000, 170f), 1.0)
    }

    @Test
    fun `step calories are in the usual range`() {
        val kcal = FitnessMath.stepCalories(8000, 70f, 170f)
        assertTrue("got $kcal", kcal in 280..340)
        assertEquals(0, FitnessMath.stepCalories(0, 70f, 170f))
    }

    @Test
    fun `counter delta handles baseline, normal reads and reboots`() {
        assertEquals(0, FitnessMath.counterDelta(null, 12_345))
        assertEquals(250, FitnessMath.counterDelta(1_000, 1_250))
        assertEquals(0, FitnessMath.counterDelta(1_000, 1_000))
        // Phone rebooted: counter restarted from 0 and is now at 300.
        assertEquals(300, FitnessMath.counterDelta(50_000, 300))
    }

    @Test
    fun `today takes the largest source and never goes down`() {
        // App was closed: the counter saw more than the Recording API so far.
        assertEquals(3_800, FitnessMath.todaySteps(current = 3_000, counterToday = 3_800, recorded = 3_200, watch = null))
        // Recording API has steps the counter missed (e.g. after a reboot).
        assertEquals(5_000, FitnessMath.todaySteps(current = 3_800, counterToday = 3_800, recorded = 5_000, watch = null))
        // A watch counted more than the phone.
        assertEquals(9_400, FitnessMath.todaySteps(current = 6_200, counterToday = 6_200, recorded = 6_000, watch = 9_400))
        // A lower source never pulls the total down.
        assertEquals(2_900, FitnessMath.todaySteps(current = 2_900, counterToday = 100, recorded = 1_500, watch = null))
    }

    @Test
    fun `a late counter reading does not double count steps the Recording API already has`() {
        // 1,000 steps on the counter; then the Recording API reports 3,000 while the counter
        // read failed. Later the counter delivers the same 2,000 steps.
        var counterToday = 1_000
        var shown = FitnessMath.todaySteps(current = 0, counterToday = counterToday, recorded = 1_000, watch = null)
        shown = FitnessMath.todaySteps(current = shown, counterToday = counterToday, recorded = 3_000, watch = null)
        counterToday += 2_000
        shown = FitnessMath.todaySteps(current = shown, counterToday = counterToday, recorded = 3_000, watch = null)
        assertEquals(3_000, shown) // the old "current + delta" logic showed 5,000
    }
}
