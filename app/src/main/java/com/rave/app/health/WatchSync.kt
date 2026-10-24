package com.rave.app.health

import com.rave.app.core.TimeProvider
import com.rave.app.data.local.dao.ActivityDao
import com.rave.app.data.local.dao.LocationPointDao
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.data.local.entity.LocationPointEntity
import com.rave.app.data.repository.ActivityProcessor
import com.rave.app.data.repository.StepRepository
import com.rave.app.domain.model.ActivityStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pulls smartwatch data from Health Connect: daily steps (merged so the larger of phone and
 * watch wins) and walks/runs, which become normal Rave activities that count for goals,
 * streaks, the feed and Wrapped. Health Connect only allows reads while the app is open,
 * so this runs whenever the step card syncs.
 */
@Singleton
class WatchSync @Inject constructor(
    private val health: HealthConnectSource,
    private val steps: StepRepository,
    private val activityDao: ActivityDao,
    private val pointDao: LocationPointDao,
    private val processor: ActivityProcessor,
    private val time: TimeProvider,
) {
    private val mutex = Mutex()

    suspend fun sync() = mutex.withLock {
        if (!health.hasPermissions()) return@withLock
        val today = time.today()
        steps.mergeWatch(health.dailySteps(DAYS, today))
        val since = time.startOfDayMs(today.minusDays((DAYS - 1).toLong()))
        health.sessions(since).forEach { importSession(it) }
    }

    private suspend fun importSession(s: WatchSession) {
        if (s.endMs <= s.startMs || activityDao.countByExternalId(s.id) > 0) return
        // Skip it if the same walk was also tracked in Rave (they overlap by more than half).
        val overlapping = activityDao.getCompletedOverlapping(s.startMs, s.endMs).any { a ->
            val end = a.endTime ?: a.startTime
            val shared = minOf(end, s.endMs) - maxOf(a.startTime, s.startMs)
            shared > (s.endMs - s.startMs) / 2
        }
        if (overlapping) return
        val id = activityDao.insert(
            ActivityEntity(
                type = s.type,
                status = ActivityStatus.COMPLETED,
                startTime = s.startMs,
                endTime = s.endMs,
                distanceM = s.distanceM,
                durationS = (s.endMs - s.startMs) / 1000,
                sourceApp = s.sourceApp,
                externalId = s.id,
            ),
        )
        if (s.route.size >= 2) {
            pointDao.insertAll(
                s.route.map { p ->
                    LocationPointEntity(
                        activityId = id, lat = p.latitude, lng = p.longitude, alt = p.altitude?.inMeters,
                        accuracy = (p.horizontalAccuracy?.inMeters ?: 10.0).toFloat(), timestamp = p.time.toEpochMilli(),
                    )
                },
            )
        }
        processor.finalize(id, endTimeMs = s.endMs)
    }

    private companion object {
        const val DAYS = 7
    }
}
