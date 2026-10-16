package com.rave.app.data.repository

import com.rave.app.core.TimeProvider
import com.rave.app.data.local.entity.StreakEntity
import com.rave.app.domain.notify.WeeklyReport
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Builds the Monday–Sunday summary used by the Sunday notification and the share screen. */
@Singleton
class WeeklyReportRepository @Inject constructor(
    private val steps: StepRepository,
    private val activities: ActivityRepository,
    private val habits: HabitRepository,
    private val streaks: StreakRepository,
    private val time: TimeProvider,
) {
    suspend fun build(weekStart: LocalDate = time.weekStart()): WeeklyReport {
        val end = weekStart.plusDays(6)
        val prevStart = weekStart.minusWeeks(1)
        val prevEnd = weekStart.minusDays(1)
        val acts = activities.observeBetween(weekStart, end).first()
        val prevActs = activities.observeBetween(prevStart, prevEnd).first()
        val habitList = habits.observeHabits().first()
        val logs = habits.observeLogsBetween(weekStart, end).first().associateBy { it.habitId to it.date }
        val today = time.today()
        var due = 0
        var done = 0
        var day = weekStart
        while (!day.isAfter(minOf(end, today))) {
            for (h in habitList) {
                if (!h.isActiveOn(day) || !h.schedule.isScheduledOn(day)) continue
                due++
                if (logs[h.id to day]?.completed == true) done++
            }
            day = day.plusDays(1)
        }
        return WeeklyReport(
            weekStart = weekStart,
            steps = steps.stepsBetween(weekStart, end),
            prevSteps = steps.stepsBetween(prevStart, prevEnd),
            distanceM = acts.sumOf { it.distanceM },
            prevDistanceM = prevActs.sumOf { it.distanceM },
            activities = acts.size,
            habitsDone = done,
            habitsDue = due,
            bestDaySteps = steps.bestDayBetween(weekStart, end),
            streak = streaks.observeStreaks().first()[StreakEntity.OVERALL]?.current ?: 0,
        )
    }
}
