package com.rave.app.data.repository

import com.rave.app.core.TimeProvider
import com.rave.app.data.local.dao.ActivityDao
import com.rave.app.data.local.dao.HabitDao
import com.rave.app.data.local.dao.HabitLogDao
import com.rave.app.data.local.dao.StatsDao
import com.rave.app.data.local.dao.StepsDao
import com.rave.app.data.local.dao.WorkoutDao
import com.rave.app.data.local.entity.StreakEntity
import com.rave.app.domain.wrapped.WrappedActivity
import com.rave.app.domain.wrapped.WrappedCalculator
import com.rave.app.domain.wrapped.WrappedHabitDone
import com.rave.app.domain.wrapped.WrappedPeriod
import com.rave.app.domain.wrapped.WrappedSummary
import com.rave.app.domain.wrapped.WrappedWorkout
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Collects a year's or a month's local data for [WrappedCalculator]. */
@Singleton
class WrappedRepository @Inject constructor(
    private val stepsDao: StepsDao,
    private val activityDao: ActivityDao,
    private val habitDao: HabitDao,
    private val logDao: HabitLogDao,
    private val workoutDao: WorkoutDao,
    private val statsDao: StatsDao,
    private val time: TimeProvider,
) {
    suspend fun summary(period: WrappedPeriod): WrappedSummary {
        val from = period.from
        val to = period.to
        val steps = stepsDao.getBetween(from.toString(), to.toString()).map { LocalDate.parse(it.date) to it.steps }
        val activities = activityDao.getCompletedBetween(time.startOfDayMs(from), time.endOfDayMs(to)).map {
            WrappedActivity(time.dateOf(it.startTime), it.distanceM, it.durationS, it.name)
        }
        val names = habitDao.getAll().associate { it.id to it.name }
        val done = logDao.getBetween(from.toEpochDay(), to.toEpochDay()).filter { it.completed }.map {
            WrappedHabitDone(names[it.habitId] ?: "", LocalDate.ofEpochDay(it.date))
        }
        val workouts = workoutDao.observeHistory().first().map { WrappedWorkout(time.dateOf(it.startTime), it.totalVolumeKg) }
        val best = statsDao.getStreaks().firstOrNull { it.habitId == StreakEntity.OVERALL }?.best ?: 0
        return WrappedCalculator.summarize(period, steps, activities, done, workouts, best)
    }
}
