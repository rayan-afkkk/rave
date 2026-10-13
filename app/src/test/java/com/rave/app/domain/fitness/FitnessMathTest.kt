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
}
