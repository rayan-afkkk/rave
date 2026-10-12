package com.rave.app.domain.habit

import com.rave.app.domain.model.ActivityGoalUnit
import com.rave.app.domain.model.ActivitySummary
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.Habit
import com.rave.app.domain.model.HabitLog
import com.rave.app.domain.model.HabitType
import java.time.LocalDate
import kotlin.math.roundToInt

/** The log value an activity-linked habit should have after today's activities. */
data class AutoTickResult(val habitId: Long, val value: Int, val completed: Boolean)

/**
 * When an activity is saved: every ACTIVITY habit of the same type that's due today gets
 * its progress updated from *all* of today's activities of that type, and is completed once
 * the goal (metres or minutes) is reached. Already-completed habits are left alone.
 */
object AutoTick {

    fun evaluate(
        habits: List<Habit>,
        todaysLogs: Map<Long, HabitLog>,
        date: LocalDate,
        todaysActivities: List<ActivitySummary>,
        type: ActivityType,
    ): List<AutoTickResult> {
        val sameType = todaysActivities.filter { it.type == type }
        if (sameType.isEmpty()) return emptyList()
        val meters = sameType.sumOf { it.distanceM }
        val minutes = sameType.sumOf { it.movingTimeS } / 60.0

        return habits
            .filter {
                it.type == HabitType.ACTIVITY && it.linkedActivityType == type &&
                    !it.archived && it.isActiveOn(date) && it.schedule.isScheduledOn(date)
            }
            .filter { todaysLogs[it.id]?.completed != true }
            .map { habit ->
                val progress = when (habit.activityGoalUnit) {
                    ActivityGoalUnit.METERS -> meters
                    ActivityGoalUnit.MINUTES -> minutes
                }
                AutoTickResult(
                    habitId = habit.id,
                    value = progress.roundToInt(),
                    completed = progress >= habit.completionTarget,
                )
            }
    }
}
