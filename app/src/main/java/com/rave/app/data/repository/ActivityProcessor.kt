package com.rave.app.data.repository

import android.content.Context
import com.rave.app.R
import com.rave.app.core.TimeProvider
import com.rave.app.data.local.dao.ActivityDao
import com.rave.app.data.local.dao.LocationPointDao
import com.rave.app.data.local.toSummary
import com.rave.app.data.local.toTrackPoint
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.habit.ActivityMarks
import com.rave.app.domain.habit.AutoTick
import com.rave.app.domain.habit.PbDetector
import com.rave.app.domain.habit.PersonalBests
import com.rave.app.domain.model.ActivityStatus
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.LogSource
import com.rave.app.domain.model.PbTag
import com.rave.app.domain.tracking.Calories
import com.rave.app.domain.tracking.TrackStats
import com.rave.app.domain.util.DayPeriod
import com.rave.app.domain.util.dayPeriodFor
import com.rave.app.work.SyncScheduler
import com.rave.app.widget.WidgetRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

data class FinishResult(
    val activityId: Long,
    val pbTags: Set<PbTag>,
    val tickedHabits: List<String>,
    val goalReached: Boolean,
)

/**
 * Turns a tracked (or auto-detected) activity into a saved one: final stats, PBs, default
 * name, habit auto-tick, badges, cloud sync.
 */
@Singleton
class ActivityProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityDao: ActivityDao,
    private val pointDao: LocationPointDao,
    private val settings: SettingsRepository,
    private val habits: HabitRepository,
    private val stats: StatsRepository,
    private val sync: SyncScheduler,
    private val widget: WidgetRefresher,
    private val time: TimeProvider,
) {
    suspend fun finalize(activityId: Long, autoDetected: Boolean = false): FinishResult? {
        val activity = activityDao.getById(activityId) ?: return null
        val points = pointDao.forActivity(activityId).map { it.toTrackPoint() }
        val prefs = settings.current()
        val now = time.nowMs()

        val distance = TrackStats.distance(points)
        // Live-tracked activities carry their active time; auto-detected ones use the point time span.
        val liveDuration = activity.durationS + (activity.resumedAt?.let { (now - it) / 1000 } ?: 0)
        val span = ((points.lastOrNull()?.timeMs ?: 0L) - (points.firstOrNull()?.timeMs ?: 0L)) / 1000
        val effectiveDuration = if (liveDuration > 0) liveDuration else span.coerceAtLeast(0)
        val moving = TrackStats.movingTimeS(points).roundToLong().coerceAtMost(effectiveDuration)
        val pace = TrackStats.paceSecondsPerKm(moving.toDouble(), distance)
        val fastest1k = TrackStats.fastestWindowS(points, 1_000.0)
        val fastest5k = TrackStats.fastestWindowS(points, 5_000.0)

        val previous = PersonalBests(
            fastest1kS = activityDao.bestFastest1k(activity.type, activityId),
            fastest5kS = activityDao.bestFastest5k(activity.type, activityId),
            longestM = activityDao.longest(activity.type, activityId),
        )
        val pbs = PbDetector.detect(previous, ActivityMarks(fastest1k, fastest5k, distance))

        val startDate = time.dateOf(activity.startTime)
        val finished = activity.copy(
            status = ActivityStatus.COMPLETED,
            endTime = now,
            distanceM = distance,
            durationS = effectiveDuration,
            movingTimeS = moving,
            avgPaceSPerKm = pace,
            calories = Calories.estimate(activity.type, prefs.weightKg.toDouble(), moving.toDouble(), distance),
            elevationGainM = TrackStats.elevationGain(points),
            name = activity.name.ifBlank { defaultName(activity.type, time.dateTimeOf(activity.startTime).hour) },
            autoDetected = autoDetected || activity.autoDetected,
            isPB = pbs.isNotEmpty(),
            pbTags = pbs.joinToString(",") { it.name },
            fastest1kS = fastest1k,
            fastest5kS = fastest5k,
            resumedAt = null,
            synced = false,
        )
        activityDao.update(finished)

        // Auto-tick activity-linked habits for the day the activity started.
        val dayHabits = habits.activeHabits()
        val dayLogs = habits.logsFor(startDate)
        val dayActivities = activityDao.getCompletedBetween(time.startOfDayMs(startDate), time.endOfDayMs(startDate)).map { it.toSummary() }
        val ticks = AutoTick.evaluate(dayHabits, dayLogs, startDate, dayActivities, activity.type)
        val source = if (finished.autoDetected) LogSource.AUTO else LogSource.GPS
        val ticked = mutableListOf<String>()
        for (t in ticks) {
            val habit = dayHabits.first { it.id == t.habitId }
            habits.setProgress(habit, startDate, t.value, source)
            if (t.completed) ticked += habit.name
        }
        val dayKm = dayActivities.sumOf { it.distanceM } / 1000.0
        val goalReached = ticked.isNotEmpty() || dayKm >= prefs.dailyGoalKm
        if (goalReached != finished.goalReached) activityDao.update(finished.copy(goalReached = goalReached))

        stats.checkBadges()
        sync.requestSync()
        widget.refresh()
        return FinishResult(activityId, pbs, ticked, goalReached)
    }

    /** "Morning walk", "Evening run"… */
    fun defaultName(type: ActivityType, hour: Int): String {
        val res = when (dayPeriodFor(hour)) {
            DayPeriod.MORNING -> if (type == ActivityType.RUN) R.string.name_morning_run else R.string.name_morning_walk
            DayPeriod.AFTERNOON -> if (type == ActivityType.RUN) R.string.name_afternoon_run else R.string.name_afternoon_walk
            DayPeriod.EVENING -> if (type == ActivityType.RUN) R.string.name_evening_run else R.string.name_evening_walk
            DayPeriod.NIGHT -> if (type == ActivityType.RUN) R.string.name_night_run else R.string.name_night_walk
        }
        return context.getString(res)
    }
}
