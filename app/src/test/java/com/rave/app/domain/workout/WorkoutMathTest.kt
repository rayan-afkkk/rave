package com.rave.app.domain.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutMathTest {
    private fun set(w: Double, r: Int, done: Boolean = true, warmup: Boolean = false, hold: Int = 0) =
        SetEntry(exerciseId = 1, weightKg = w, reps = r, durationS = hold, done = done, warmup = warmup)

    @Test
    fun `epley one rep max`() {
        assertEquals(100.0, WorkoutMath.estimated1rm(100.0, 1), 1e-9)
        assertEquals(120.0, WorkoutMath.estimated1rm(100.0, 6), 1e-9)
        assertEquals(0.0, WorkoutMath.estimated1rm(100.0, 0), 1e-9)
    }

    @Test
    fun `volume ignores warm-ups and unfinished sets`() {
        val sets = listOf(set(40.0, 10, warmup = true), set(60.0, 8), set(60.0, 8), set(60.0, 6, done = false))
        assertEquals(960.0, WorkoutMath.volume(sets), 1e-9)
    }

    @Test
    fun `first session is never a PR`() {
        assertTrue(WorkoutMath.detectPrs(ExerciseBests(), listOf(set(100.0, 5)), ExerciseKind.WEIGHT_REPS).isEmpty())
    }

    @Test
    fun `detects heaviest, 1RM and set volume PRs`() {
        val previous = WorkoutMath.bests(listOf(set(80.0, 8), set(85.0, 5)))
        val prs = WorkoutMath.detectPrs(previous, listOf(set(87.5, 3)), ExerciseKind.WEIGHT_REPS)
        assertEquals(setOf(SetPr.HEAVIEST), prs) // 87.5×3 → e1RM 96.25 < 101.3 from 80×8
        val prs2 = WorkoutMath.detectPrs(previous, listOf(set(82.5, 9)), ExerciseKind.WEIGHT_REPS)
        assertEquals(setOf(SetPr.BEST_1RM, SetPr.BEST_VOLUME), prs2)
    }

    @Test
    fun `bodyweight and hold PRs`() {
        val prev = WorkoutMath.bests(listOf(set(0.0, 12), set(0.0, 0, hold = 60)))
        assertEquals(setOf(SetPr.MOST_REPS), WorkoutMath.detectPrs(prev, listOf(set(0.0, 15)), ExerciseKind.BODYWEIGHT_REPS))
        assertEquals(setOf(SetPr.LONGEST_HOLD), WorkoutMath.detectPrs(prev, listOf(set(0.0, 0, hold = 75)), ExerciseKind.DURATION))
    }
}
