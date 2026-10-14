package com.rave.app.data.repository

import com.rave.app.core.TimeProvider
import com.rave.app.data.local.dao.ActivityDao
import com.rave.app.data.local.dao.HabitLogDao
import com.rave.app.data.local.dao.StepsDao
import com.rave.app.data.local.dao.WorkoutDao
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.buddy.BuddyEngine
import com.rave.app.domain.buddy.BuddyState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

/** Live state of the Rave buddy from all-time totals plus today's activity. */
@Singleton
class BuddyRepository @Inject constructor(
    private val stepsDao: StepsDao,
    private val activityDao: ActivityDao,
    private val logDao: HabitLogDao,
    private val workoutDao: WorkoutDao,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<BuddyState> {
        val totals = combine(
            stepsDao.observeTotal(), logDao.observeCompletedCount(), activityDao.observeTotalDistance(), workoutDao.observeCount(),
        ) { steps, habits, meters, workouts -> BuddyEngine.xp(steps, habits, meters / 1000.0, workouts) }
        val today = time.todayFlow.flatMapLatest { day ->
            combine(
                stepsDao.observe(day.toString()),
                logDao.observeCompletedCountBetween(day.toEpochDay(), day.toEpochDay()),
                activityDao.observeCompletedBetween(time.startOfDayMs(day), time.endOfDayMs(day)),
            ) { steps, habitsDone, acts -> Triple(steps?.steps ?: 0, habitsDone > 0, acts.isNotEmpty()) }
        }
        return combine(totals, today, settings.settings) { xp, (steps, habitDone, moved), prefs ->
            BuddyEngine.state(xp, steps, prefs.dailyStepGoal, habitDone || moved, time.now().hour)
        }
    }
}
