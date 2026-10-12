package com.rave.app.domain.model

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
