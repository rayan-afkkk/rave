package com.rave.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** How a habit is completed. */
enum class HabitType {
    /** Done / not done. */
    CHECKBOX,
    /** Count up to a target, e.g. 8 glasses of water. */
    COUNTER,
    /** Run a built-in timer for N minutes. */
    TIMED,
    /** Auto-completed by a tracked walk/run that meets the goal. */
    ACTIVITY,
    /** Auto-completed by finishing a strength workout (target = number of working sets, min 1). */
    WORKOUT,
}

/** When a habit is due. */
enum class ScheduleType {
    EVERY_DAY,
    /** Only on the weekdays in the days mask. */
    SPECIFIC_DAYS,
    /** Any N days in a week. */
    TIMES_PER_WEEK,
}

/** Soft tile colours available in the habit colour picker. */
enum class HabitColor { PEACH, GREEN, PINK, BLUE }

/** Goal unit for activity-linked habits. */
enum class ActivityGoalUnit { METERS, MINUTES }

/** Where a habit log came from. */
enum class LogSource { MANUAL, GPS, AUTO, WORKOUT }

data class HabitSchedule(
    val type: ScheduleType = ScheduleType.EVERY_DAY,
    /** Bit 0 = Monday … bit 6 = Sunday. Only used for SPECIFIC_DAYS. */
    val daysMask: Int = ALL_DAYS,
    /** Only used for TIMES_PER_WEEK. */
    val timesPerWeek: Int = 0,
) {
    /** Whether the habit appears in the routine on [date]. Weekly-count habits show every day. */
    fun isScheduledOn(date: LocalDate): Boolean = when (type) {
        ScheduleType.EVERY_DAY, ScheduleType.TIMES_PER_WEEK -> true
        ScheduleType.SPECIFIC_DAYS -> includes(date.dayOfWeek)
    }

    fun includes(day: DayOfWeek): Boolean = daysMask and bitOf(day) != 0

    companion object {
        const val ALL_DAYS = 0b111_1111
        fun bitOf(day: DayOfWeek): Int = 1 shl (day.value - 1)
        fun maskOf(days: Collection<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or bitOf(d) }
    }
}

data class Habit(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: HabitColor,
    val type: HabitType,
    /** Counter target, minutes for TIMED, metres/minutes for ACTIVITY (see [activityGoalUnit]). */
    val target: Int = 1,
    /** Free-text unit label for counters, e.g. "glasses". */
    val unit: String? = null,
    val schedule: HabitSchedule = HabitSchedule(),
    val reminderTimes: List<LocalTime> = emptyList(),
    val remindersEnabled: Boolean = false,
    val linkedActivityType: ActivityType? = null,
    val activityGoalUnit: ActivityGoalUnit = ActivityGoalUnit.METERS,
    /** Firestore id when this habit is part of a shared streak with a friend. */
    val sharedStreakId: String? = null,
    val sharedWithName: String? = null,
    val archived: Boolean = false,
    val createdDate: LocalDate,
    val archivedDate: LocalDate? = null,
) {
    /** The value a log needs to reach to count as completed. */
    val completionTarget: Int get() = if (type == HabitType.CHECKBOX) 1 else target.coerceAtLeast(1)

    fun isActiveOn(date: LocalDate): Boolean =
        !date.isBefore(createdDate) && (archivedDate == null || date.isBefore(archivedDate))
}

data class HabitLog(
    val id: Long = 0,
    val habitId: Long,
    val date: LocalDate,
    val value: Int = 0,
    val completed: Boolean = false,
    val skipped: Boolean = false,
    val source: LogSource = LogSource.MANUAL,
)
