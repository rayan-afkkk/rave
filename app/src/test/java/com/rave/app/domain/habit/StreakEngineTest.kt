package com.rave.app.domain.habit

import com.rave.app.domain.model.Habit
import com.rave.app.domain.model.HabitColor
import com.rave.app.domain.model.HabitLog
import com.rave.app.domain.model.HabitSchedule
import com.rave.app.domain.model.HabitType
import com.rave.app.domain.model.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StreakEngineTest {
    // 2026-01-05 is a Monday.
    private val monday = LocalDate.of(2026, 1, 5)

    private fun habit(id: Long, schedule: HabitSchedule = HabitSchedule(), created: LocalDate = monday) =
        Habit(id = id, name = "h$id", icon = "check", color = HabitColor.PEACH, type = HabitType.CHECKBOX,
            schedule = schedule, createdDate = created)

    private fun done(id: Long, vararg offsets: Long) = offsets.map { HabitLog(habitId = id, date = monday.plusDays(it), value = 1, completed = true) }
    private fun skipped(id: Long, vararg offsets: Long) = offsets.map { HabitLog(habitId = id, date = monday.plusDays(it), skipped = true) }

    @Test
    fun `consecutive completions grow the streak`() {
        val r = StreakEngine.replay(listOf(habit(1)), done(1, 0, 1, 2), through = monday.plusDays(2))
        assertEquals(3, r.perHabit.getValue(1).current)
        assertEquals(3, r.overall.current)
    }

    @Test
    fun `a missed day resets when no freeze is available`() {
        val r = StreakEngine.replay(listOf(habit(1)), done(1, 0, 1, 3), through = monday.plusDays(3))
        assertEquals(1, r.perHabit.getValue(1).current)
        assertEquals(2, r.perHabit.getValue(1).best)
        assertEquals(0, r.freezesRemaining)
    }

    @Test
    fun `seven day streak earns a freeze which then covers a missed day`() {
        val logs = done(1, 0, 1, 2, 3, 4, 5, 6, 8, 9) // day 7 missed
        val r = StreakEngine.replay(listOf(habit(1)), logs, through = monday.plusDays(9))
        assertEquals(1, r.freezesEarned)
        assertEquals(listOf(monday.plusDays(7)), r.freezesUsedOn)
        assertEquals(0, r.freezesRemaining)
        assertEquals(9, r.perHabit.getValue(1).current)
        assertEquals(9, r.overall.current)
    }

    @Test
    fun `freezes are capped at three`() {
        val logs = done(1, *(0L until 35L).toList().toLongArray())
        val r = StreakEngine.replay(listOf(habit(1)), logs, through = monday.plusDays(34))
        assertEquals(StreakEngine.MAX_FREEZES, r.freezesRemaining)
        assertEquals(35, r.overall.current)
    }

    @Test
    fun `bonus freezes protect a streak`() {
        val r = StreakEngine.replay(listOf(habit(1)), done(1, 0, 2), through = monday.plusDays(2), bonusFreezes = 1)
        assertEquals(2, r.perHabit.getValue(1).current)
        assertEquals(0, r.freezesRemaining)
    }

    @Test
    fun `skipping keeps the streak without growing it`() {
        val logs = done(1, 0, 2) + skipped(1, 1)
        val r = StreakEngine.replay(listOf(habit(1)), logs, through = monday.plusDays(2))
        assertEquals(2, r.perHabit.getValue(1).current)
    }

    @Test
    fun `unscheduled days are neutral`() {
        val weekdays = HabitSchedule(ScheduleType.SPECIFIC_DAYS, HabitSchedule.maskOf(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)))
        val r = StreakEngine.replay(listOf(habit(1, weekdays)), done(1, 0, 2), through = monday.plusDays(3))
        assertEquals(2, r.perHabit.getValue(1).current)
    }

    @Test
    fun `times per week habits are judged on sunday`() {
        val thrice = HabitSchedule(ScheduleType.TIMES_PER_WEEK, timesPerWeek = 3)
        val ok = StreakEngine.replay(listOf(habit(1, thrice)), done(1, 0, 3, 5), through = monday.plusDays(6))
        assertEquals(3, ok.perHabit.getValue(1).current)
        val short = StreakEngine.replay(listOf(habit(1, thrice)), done(1, 0, 3), through = monday.plusDays(6))
        assertEquals(0, short.perHabit.getValue(1).current)
    }

    @Test
    fun `habits created later are not missed before creation`() {
        val late = habit(2, created = monday.plusDays(2))
        val logs = done(1, 0, 1, 2) + done(2, 2)
        val r = StreakEngine.replay(listOf(habit(1), late), logs, through = monday.plusDays(2))
        assertEquals(3, r.overall.current)
        assertEquals(1, r.perHabit.getValue(2).current)
    }

    @Test
    fun `a break is reported with the streak that was lost`() {
        val r = StreakEngine.replay(listOf(habit(1)), done(1, 0, 1, 2, 4), through = monday.plusDays(4))
        assertEquals(monday.plusDays(3), r.lastBreak)
        assertEquals(3, r.lostStreak)
        assertEquals(1, r.overall.current)
    }

    @Test
    fun `repairing the missed day restores the streak without using a freeze`() {
        val logs = done(1, 0, 1, 2, 4)
        val r = StreakEngine.replay(listOf(habit(1)), logs, through = monday.plusDays(4), repairedDays = setOf(monday.plusDays(3)))
        assertEquals(4, r.overall.current)
        assertEquals(4, r.perHabit.getValue(1).current)
        assertEquals(null, r.lastBreak)
        assertEquals(0, r.freezesUsedOn.size)
    }

    @Test
    fun `monthly freeze covers a miss`() {
        // Created Jan 5 (first tracked day grants one); Jan 8 missed is covered.
        val r = StreakEngine.replay(listOf(habit(1)), done(1, 0, 1, 2, 4), through = monday.plusDays(4), monthlyFreeze = true)
        assertEquals(4, r.overall.current)
        assertEquals(listOf(monday.plusDays(3)), r.freezesUsedOn)
    }

    @Test
    fun `monthly freeze is granted again on the first of the month`() {
        val start = LocalDate.of(2026, 1, 30)
        val h = habit(1, created = start)
        // Jan 30 done, Jan 31 missed (uses the starting freeze), Feb 1 done, Feb 2 missed (uses the Feb freeze).
        val logs = listOf(start, start.plusDays(2)).map { HabitLog(habitId = 1, date = it, value = 1, completed = true) }
        val r = StreakEngine.replay(listOf(h), logs, through = start.plusDays(3), monthlyFreeze = true)
        assertEquals(2, r.freezesUsedOn.size)
        assertEquals(2, r.overall.current)
    }

    @Test
    fun `display streak adds today when done`() {
        assertEquals(5, StreakEngine.displayStreak(StreakState(current = 4), doneToday = true))
        assertEquals(4, StreakEngine.displayStreak(StreakState(current = 4), doneToday = false))
    }
}
