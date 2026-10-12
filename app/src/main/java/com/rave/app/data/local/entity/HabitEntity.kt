package com.rave.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.HabitColor
import com.rave.app.domain.model.HabitType
import com.rave.app.domain.model.ScheduleType

/**
 * A habit the user tracks. Enums are stored by name (Room's built-in enum support).
 *
 * Schedule is flattened into three columns:
 * - [scheduleType] EVERY_DAY / SPECIFIC_DAYS / TIMES_PER_WEEK
 * - [scheduleDaysMask] bit 0 = Monday … bit 6 = Sunday (SPECIFIC_DAYS only)
 * - [timesPerWeek] target count (TIMES_PER_WEEK only)
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Key into the app's icon set (resolved in the UI layer). */
    val icon: String,
    val color: HabitColor,
    val type: HabitType,
    /** Counter target, minutes for TIMED, or km×1000 / minutes for ACTIVITY (see [unit]). */
    val target: Int = 1,
    /** e.g. "glasses", "min", "m". Null for CHECKBOX. */
    val unit: String? = null,
    val scheduleType: ScheduleType = ScheduleType.EVERY_DAY,
    val scheduleDaysMask: Int = 0,
    val timesPerWeek: Int = 0,
    /** Local times as "HH:mm". */
    val reminderTimes: List<String> = emptyList(),
    val remindersEnabled: Boolean = false,
    val linkedActivityType: ActivityType? = null,
    val archived: Boolean = false,
    val createdAt: Long,
)
