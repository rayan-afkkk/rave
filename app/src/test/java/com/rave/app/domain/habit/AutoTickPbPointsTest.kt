package com.rave.app.domain.habit

import com.rave.app.domain.model.ActivityGoalUnit
import com.rave.app.domain.model.ActivitySummary
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.Habit
import com.rave.app.domain.model.HabitColor
import com.rave.app.domain.model.HabitLog
import com.rave.app.domain.model.HabitType
import com.rave.app.domain.model.PbTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AutoTickPbPointsTest {
    private val today = LocalDate.of(2026, 3, 2)

    private fun activityHabit(id: Long, type: ActivityType, target: Int, unit: ActivityGoalUnit = ActivityGoalUnit.METERS) =
        Habit(id = id, name = "h$id", icon = "walk", color = HabitColor.GREEN, type = HabitType.ACTIVITY,
            target = target, linkedActivityType = type, activityGoalUnit = unit, createdDate = today.minusDays(3))

    private fun walk(m: Double, s: Long = 1800) = ActivitySummary(1, ActivityType.WALK, 0, m, s, autoDetected = false)

    @Test
    fun `completes walk habit when goal reached`() {
        val result = AutoTick.evaluate(listOf(activityHabit(1, ActivityType.WALK, 3000)), emptyMap(), today, listOf(walk(3200.0)), ActivityType.WALK)
        assertEquals(listOf(AutoTickResult(1, 3200, true)), result)
    }

    @Test
    fun `adds up several activities and records partial progress`() {
        val habits = listOf(activityHabit(1, ActivityType.WALK, 5000))
        val result = AutoTick.evaluate(habits, emptyMap(), today, listOf(walk(2000.0), walk(1500.0)), ActivityType.WALK)
        assertEquals(listOf(AutoTickResult(1, 3500, false)), result)
    }

    @Test
    fun `ignores other activity types and finished habits`() {
        val habits = listOf(activityHabit(1, ActivityType.RUN, 1000), activityHabit(2, ActivityType.WALK, 1000))
        val logs = mapOf(2L to HabitLog(habitId = 2, date = today, value = 1000, completed = true))
        assertTrue(AutoTick.evaluate(habits, logs, today, listOf(walk(4000.0)), ActivityType.WALK).isEmpty())
    }

    @Test
    fun `minute goals use moving time`() {
        val habits = listOf(activityHabit(1, ActivityType.WALK, 30, ActivityGoalUnit.MINUTES))
        val result = AutoTick.evaluate(habits, emptyMap(), today, listOf(walk(2500.0, s = 31 * 60)), ActivityType.WALK)
        assertEquals(listOf(AutoTickResult(1, 31, true)), result)
    }

    @Test
    fun `pb detection needs a previous mark to beat`() {
        val marks = ActivityMarks(fastest1kS = 300.0, fastest5kS = 1600.0, distanceM = 6000.0)
        assertTrue(PbDetector.detect(PersonalBests(), marks).isEmpty())
        val prev = PersonalBests(fastest1kS = 310.0, fastest5kS = 1590.0, longestM = 5500.0)
        assertEquals(setOf(PbTag.FASTEST_1K, PbTag.LONGEST_DISTANCE), PbDetector.detect(prev, marks))
    }

    @Test
    fun `points and levels`() {
        assertEquals(4 * 10 + 32 + 50, Points.compute(habitsDone = 4, metersTracked = 3_250.0, competitionWins = 1))
        assertEquals(1, Points.level(499))
        assertEquals(2, Points.level(500))
    }

    @Test
    fun `badges unlock at thresholds`() {
        val earned = BadgeEvaluator.earned(BadgeInputs(totalActivities = 3, longestActivityM = 5_100.0, bestStreak = 8, invitesAccepted = 3))
        assertEquals(setOf(BadgeId.FIRST_ACTIVITY, BadgeId.FIRST_5K, BadgeId.STREAK_7, BadgeId.SOCIAL), earned)
    }
}
