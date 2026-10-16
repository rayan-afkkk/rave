package com.rave.app.domain.habit

import com.rave.app.domain.model.Habit
import com.rave.app.domain.model.HabitLog
import com.rave.app.domain.model.ScheduleType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

data class StreakState(
    val current: Int = 0,
    val best: Int = 0,
    val lastCompleted: LocalDate? = null,
)

data class StreakReplay(
    val perHabit: Map<Long, StreakState>,
    /** Consecutive days where every due habit was done (or skipped) and at least one was done. */
    val overall: StreakState,
    val freezesRemaining: Int,
    val freezesUsedOn: List<LocalDate>,
    val freezesEarned: Int,
    /** Most recent day the overall streak reset because of a miss (no freeze, no repair). */
    val lastBreak: LocalDate? = null,
    /** Overall streak that was lost on [lastBreak]. */
    val lostStreak: Int = 0,
)

/**
 * Streak + freeze rules. Streaks are *replayed* from the full log history, which makes the
 * result deterministic and lets the user edit yesterday without corrupting state.
 *
 * Per evaluated day:
 * - A due daily habit with no completion and no skip is MISSED.
 * - TIMES_PER_WEEK habits are judged on Sunday: fewer completions than required = MISSED.
 * - If anything was missed and a freeze is available, one freeze covers the whole day
 *   (no streak resets, nothing increments for the missed habits).
 * - Otherwise MISSED resets that habit's streak and the overall streak.
 * - Every 7th consecutive overall day earns a freeze (max [MAX_FREEZES]).
 * - [monthlyFreezes] free freezes are added on the first tracked day and on the 1st of every
 *   month (1 free, 3 with Rave Pro; still capped at [MAX_FREEZES]).
 * - A day in [repairedDays] that would break the streak is covered without using a freeze
 *   (streak repair).
 * - Skipping keeps a streak but doesn't grow it. Days with nothing due are neutral.
 */
object StreakEngine {
    const val MAX_FREEZES = 3
    const val DAYS_PER_FREEZE = 7

    private enum class DayStatus { DONE, SKIPPED, MISSED, NONE }

    fun replay(
        habits: List<Habit>,
        logs: List<HabitLog>,
        through: LocalDate,
        bonusFreezes: Int = 0,
        monthlyFreezes: Int = 0,
        repairedDays: Set<LocalDate> = emptySet(),
    ): StreakReplay {
        val perHabit = habits.associate { it.id to StreakState() }.toMutableMap()
        var overall = StreakState()
        var freezes = bonusFreezes.coerceIn(0, MAX_FREEZES)
        var earned = 0
        val used = mutableListOf<LocalDate>()
        var lastBreak: LocalDate? = null
        var lostStreak = 0
        val start = habits.minOfOrNull { it.createdDate }
        if (start == null || start.isAfter(through)) {
            return StreakReplay(perHabit, overall, freezes, used, earned)
        }

        val logIndex: Map<Pair<Long, LocalDate>, HabitLog> = logs.associateBy { it.habitId to it.date }

        var day: LocalDate = start
        while (!day.isAfter(through)) {
            if (monthlyFreezes > 0 && (day == start || day.dayOfMonth == 1)) freezes = minOf(MAX_FREEZES, freezes + monthlyFreezes)
            val statuses = mutableMapOf<Long, DayStatus>()
            for (habit in habits) {
                if (!habit.isActiveOn(day)) continue
                statuses[habit.id] = statusFor(habit, day, logIndex)
            }

            val anyMissed = statuses.values.any { it == DayStatus.MISSED }
            val repaired = anyMissed && day in repairedDays
            val frozen = repaired || (anyMissed && freezes > 0)
            if (frozen && !repaired) {
                freezes--
                used += day
            }

            for ((id, status) in statuses) {
                val s = perHabit.getValue(id)
                perHabit[id] = when (status) {
                    DayStatus.DONE -> s.copy(current = s.current + 1, best = maxOf(s.best, s.current + 1), lastCompleted = day)
                    DayStatus.MISSED -> if (frozen) s else s.copy(current = 0)
                    DayStatus.SKIPPED, DayStatus.NONE -> s
                }
            }

            val anyDone = statuses.values.any { it == DayStatus.DONE }
            overall = when {
                anyMissed && frozen -> overall
                anyMissed -> {
                    if (overall.current > 0) {
                        lastBreak = day
                        lostStreak = overall.current
                    }
                    overall.copy(current = 0)
                }
                anyDone -> {
                    val next = overall.current + 1
                    if (next % DAYS_PER_FREEZE == 0 && freezes < MAX_FREEZES) {
                        freezes++
                        earned++
                    }
                    overall.copy(current = next, best = maxOf(overall.best, next), lastCompleted = day)
                }
                else -> overall
            }
            day = day.plusDays(1)
        }
        return StreakReplay(perHabit, overall, freezes, used, earned, lastBreak, lostStreak)
    }

    private fun statusFor(habit: Habit, day: LocalDate, logs: Map<Pair<Long, LocalDate>, HabitLog>): DayStatus {
        val log = logs[habit.id to day]
        if (habit.schedule.type == ScheduleType.TIMES_PER_WEEK) {
            // A completion today counts as DONE; a weekly shortfall is only judged on Sunday.
            val doneToday = log?.completed == true
            if (day.dayOfWeek == DayOfWeek.SUNDAY && weeklyShortfall(habit, day, logs)) return DayStatus.MISSED
            return if (doneToday) DayStatus.DONE else DayStatus.NONE
        }
        if (!habit.schedule.isScheduledOn(day)) return if (log?.completed == true) DayStatus.DONE else DayStatus.NONE
        return when {
            log?.completed == true -> DayStatus.DONE
            log?.skipped == true -> DayStatus.SKIPPED
            else -> DayStatus.MISSED
        }
    }

    /** True if the week ending on [sunday] had fewer completions than required (pro-rated for new habits). */
    private fun weeklyShortfall(habit: Habit, sunday: LocalDate, logs: Map<Pair<Long, LocalDate>, HabitLog>): Boolean {
        val monday = sunday.minusDays(6)
        val from = if (habit.createdDate.isAfter(monday)) habit.createdDate else monday
        val activeDays = ChronoUnit.DAYS.between(from, sunday) + 1
        val required = ceil(habit.schedule.timesPerWeek * activeDays / 7.0).toInt()
        var count = 0
        var d = from
        while (!d.isAfter(sunday)) {
            if (logs[habit.id to d]?.completed == true) count++
            d = d.plusDays(1)
        }
        return count < required
    }

    /**
     * Streak to *display* today: the replayed streak (through yesterday) plus one if the
     * habit is already done today.
     */
    fun displayStreak(throughYesterday: StreakState, doneToday: Boolean): Int =
        throughYesterday.current + if (doneToday) 1 else 0
}
