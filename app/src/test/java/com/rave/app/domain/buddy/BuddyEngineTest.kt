package com.rave.app.domain.buddy

import org.junit.Assert.assertEquals
import org.junit.Test

class BuddyEngineTest {

    @Test
    fun `xp adds up from every kind of activity`() {
        // 12,345 steps → 123, 3 habits → 30, 2.5 km → 50, 1 workout → 25
        assertEquals(228, BuddyEngine.xp(12_345, 3, 2.5, 1))
    }

    @Test
    fun `levels follow the gentle curve`() {
        assertEquals(1, BuddyEngine.state(99, 0, 8000, false, 10).level)
        assertEquals(2, BuddyEngine.state(100, 0, 8000, false, 10).level)
        // 100 + 150 = 250 reaches level 3 (Buddy stage)
        val s = BuddyEngine.state(260, 0, 8000, false, 10)
        assertEquals(3, s.level)
        assertEquals(BuddyStage.BUDDY, s.stage)
        assertEquals(10, s.xpIntoLevel)
        assertEquals(200, s.xpForNextLevel)
    }

    @Test
    fun `mood reflects today without punishing`() {
        assertEquals(BuddyMood.PUMPED, BuddyEngine.state(0, 9000, 8000, false, 12).mood)
        assertEquals(BuddyMood.HAPPY, BuddyEngine.state(0, 100, 8000, true, 12).mood)
        assertEquals(BuddyMood.SLEEPY, BuddyEngine.state(0, 500, 8000, false, 20).mood)
        assertEquals(BuddyMood.OKAY, BuddyEngine.state(0, 500, 8000, false, 9).mood)
    }
}
