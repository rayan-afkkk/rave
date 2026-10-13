package com.rave.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rave.app.domain.model.ActivityGoalUnit
import com.rave.app.domain.model.ActivityStatus
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.HabitColor
import com.rave.app.domain.model.HabitType
import com.rave.app.domain.model.LogSource
import com.rave.app.domain.model.ScheduleType

/*
 * Room entities. Enums are stored by name (Room's built-in enum support).
 * Dates are stored as epoch days (LocalDate.toEpochDay), instants as epoch millis.
 */

/**
 * A habit. Schedule is flattened into three columns:
 * [scheduleType], [scheduleDaysMask] (bit 0 = Monday … bit 6 = Sunday) and [timesPerWeek].
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val color: HabitColor,
    val type: HabitType,
    val target: Int = 1,
    val unit: String? = null,
    val scheduleType: ScheduleType = ScheduleType.EVERY_DAY,
    val scheduleDaysMask: Int = 0b111_1111,
    val timesPerWeek: Int = 0,
    /** Local times as "HH:mm". */
    val reminderTimes: List<String> = emptyList(),
    val remindersEnabled: Boolean = false,
    val linkedActivityType: ActivityType? = null,
    val activityGoalUnit: ActivityGoalUnit = ActivityGoalUnit.METERS,
    val sharedStreakId: String? = null,
    val sharedWithName: String? = null,
    val archived: Boolean = false,
    val createdAt: Long,
    /** Epoch day the habit started counting (local date of creation). */
    val createdEpochDay: Long,
    val archivedEpochDay: Long? = null,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["habitId", "date"], unique = true), Index("date")],
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    /** Epoch day. */
    val date: Long,
    val value: Int = 0,
    val completed: Boolean = false,
    val skipped: Boolean = false,
    val source: LogSource = LogSource.MANUAL,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "activities", indices = [Index("startTime"), Index("status")])
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ActivityType,
    val status: ActivityStatus,
    val startTime: Long,
    val endTime: Long? = null,
    val distanceM: Double = 0.0,
    /** Active (not manually paused) time in seconds, accumulated up to the last pause. */
    val durationS: Long = 0,
    val movingTimeS: Long = 0,
    val avgPaceSPerKm: Double? = null,
    val calories: Int = 0,
    val elevationGainM: Double = 0.0,
    val name: String = "",
    val note: String = "",
    val favorite: Boolean = false,
    val autoDetected: Boolean = false,
    val isPB: Boolean = false,
    /** Comma-separated [com.rave.app.domain.model.PbTag] names. */
    val pbTags: String = "",
    val fastest1kS: Double? = null,
    val fastest5kS: Double? = null,
    val goalReached: Boolean = false,
    /** While ACTIVE: wall-clock time of the last start/resume (for live duration). */
    val resumedAt: Long? = null,
    /** Current GPS segment; incremented on every resume so pauses aren't counted. */
    val segment: Int = 0,
    /** Favourite route shown as a guide line while tracking. */
    val guideActivityId: Long? = null,
    val synced: Boolean = false,
    val remoteId: String? = null,
)

@Entity(
    tableName = "location_points",
    foreignKeys = [ForeignKey(entity = ActivityEntity::class, parentColumns = ["id"], childColumns = ["activityId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("activityId")],
)
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: Long,
    val lat: Double,
    val lng: Double,
    val alt: Double? = null,
    val accuracy: Float,
    val speed: Float? = null,
    val timestamp: Long,
    val segment: Int = 0,
)

/** Cached streak per habit. [habitId] == [OVERALL] holds the overall streak. */
@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey val habitId: Long,
    val current: Int,
    val best: Int,
    val lastCompletedDate: Long? = null,
) {
    companion object {
        const val OVERALL = -1L
    }
}

/** Single row (id = 0). */
@Entity(tableName = "freeze_inventory")
data class FreezeInventoryEntity(
    @PrimaryKey val id: Int = 0,
    /** Freezes available now (derived by the streak replay). */
    val count: Int = 0,
    /** Extra freezes granted outside streaks (e.g. invite reward). */
    val bonusGranted: Int = 0,
    /** Epoch day of the most recent freeze use we've already notified about. */
    val lastNotifiedUseDay: Long? = null,
)

/** Single row (id = 0) of counters that aren't derivable from other tables. */
@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 0,
    val invitesAccepted: Int = 0,
    val inviteRewardClaimed: Boolean = false,
    val invitedByUid: String? = null,
)

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val id: String,
    val earnedAt: Long,
)

@Entity(tableName = "privacy_zones")
data class PrivacyZoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lat: Double,
    val lng: Double,
    val radiusM: Double,
    val label: String,
)

@Entity(tableName = "competition_wins")
data class CompetitionWinEntity(
    @PrimaryKey val competitionId: String,
    val name: String,
    val wonAt: Long,
)

// ---- Strength workouts (set tracker) ----

@Entity(tableName = "exercises", indices = [Index(value = ["name"], unique = true)])
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscle: com.rave.app.domain.workout.MuscleGroup,
    val kind: com.rave.app.domain.workout.ExerciseKind,
    val custom: Boolean = false,
    val archived: Boolean = false,
)

/** A workout session. [endTime] == null means it's still in progress (survives process death). */
@Entity(tableName = "workouts", indices = [Index("startTime")])
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val note: String = "",
    val startTime: Long,
    val endTime: Long? = null,
    val totalVolumeKg: Double = 0.0,
    val workingSets: Int = 0,
    /** Number of exercise PRs set in this workout. */
    val prCount: Int = 0,
    /** "exerciseId:TAG|TAG;exerciseId:TAG" — see WorkoutRepository. */
    val prTags: String = "",
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(entity = WorkoutEntity::class, parentColumns = ["id"], childColumns = ["workoutId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    /** Position of the exercise within the workout (exercises keep the order they were added). */
    val exerciseOrder: Int,
    /** Position of the set within the exercise. */
    val setOrder: Int,
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val durationS: Int = 0,
    val warmup: Boolean = false,
    val done: Boolean = false,
    val completedAt: Long? = null,
)

/** Steps counted on one calendar day (date = ISO yyyy-MM-dd, local time). */
@Entity(tableName = "daily_steps")
data class DailyStepsEntity(
    @PrimaryKey val date: String,
    val steps: Int,
    val updatedAt: Long,
)
