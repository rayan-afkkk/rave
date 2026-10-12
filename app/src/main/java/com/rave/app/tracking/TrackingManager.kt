package com.rave.app.tracking

import android.content.Context
import android.location.Location
import com.rave.app.R
import com.rave.app.core.ApplicationScope
import com.rave.app.core.TimeProvider
import com.rave.app.data.local.dao.ActivityDao
import com.rave.app.data.local.dao.LocationPointDao
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.data.local.entity.LocationPointEntity
import com.rave.app.data.local.toTrackPoint
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.ActivityProcessor
import com.rave.app.data.repository.FinishResult
import com.rave.app.data.repository.HabitRepository
import com.rave.app.domain.model.ActivityGoalUnit
import com.rave.app.domain.model.ActivityStatus
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.model.HabitType
import com.rave.app.domain.model.LatLngPoint
import com.rave.app.domain.model.TrackPoint
import com.rave.app.domain.model.UserSettings
import com.rave.app.domain.tracking.Calories
import com.rave.app.domain.tracking.FilterResult
import com.rave.app.domain.tracking.TrackFilter
import com.rave.app.domain.tracking.TrackStats
import com.rave.app.domain.util.Format
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Owns the live tracking session. Every accepted GPS point is written to Room immediately,
 * so a session survives process death and is restored by [restore].
 */
@Singleton
class TrackingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityDao: ActivityDao,
    private val pointDao: LocationPointDao,
    private val settingsRepo: SettingsRepository,
    private val habits: HabitRepository,
    private val processor: ActivityProcessor,
    private val voice: VoiceCoach,
    private val time: TimeProvider,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val filter = TrackFilter()
    private val mutex = Mutex()

    private val _state = MutableStateFlow(TrackingState())
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    private val _finished = MutableSharedFlow<FinishResult>(extraBufferCapacity = 1)
    /** Emits when a session is finished (from the UI or the notification). */
    val finished: SharedFlow<FinishResult> = _finished.asSharedFlow()

    private val _locationMode = MutableStateFlow(LocationMode(NORMAL_INTERVAL_MS, highAccuracy = true))
    val locationMode: StateFlow<LocationMode> = _locationMode.asStateFlow()

    private var entity: ActivityEntity? = null
    private var lastAccepted: TrackPoint? = null
    private var lastFixAt = 0L
    private var lastMovementAt = 0L
    private var slowSince: Long? = null
    private var lastSplitIndex = 0
    private var lastSplitElapsedMs = 0L
    private var settings = UserSettings()
    private var ticker: Job? = null

    /** Starts a new session (or returns the one already running). */
    suspend fun start(type: ActivityType, guideActivityId: Long?): Long = mutex.withLock {
        _state.value.activityId?.let { return it }
        activityDao.getInProgress()?.let { existing ->
            restoreLocked(existing)
            return existing.id
        }
        settings = settingsRepo.current()
        val now = time.nowMs()
        val created = ActivityEntity(
            type = type,
            status = ActivityStatus.ACTIVE,
            startTime = now,
            resumedAt = now,
            guideActivityId = guideActivityId?.takeIf { it > 0 },
        )
        val id = activityDao.insert(created)
        entity = created.copy(id = id)
        resetCounters(now)
        val (goal, goalName) = goalFor(type)
        _state.value = TrackingState(
            activityId = id, type = type, status = TrackingStatus.ACTIVE, startedAt = now, resumedAt = now, nowMs = now,
            goalM = goal, goalHabitName = goalName, guide = loadGuide(guideActivityId),
        )
        if (settings.voiceFeedback) voice.warmUp()
        TrackingService.start(context)
        startTicker()
        id
    }

    /** Restores an in-progress session after process death. Returns true if one exists. */
    suspend fun restore(): Boolean = mutex.withLock {
        if (_state.value.activityId != null) return true
        val existing = activityDao.getInProgress() ?: return false
        restoreLocked(existing)
        true
    }

    private suspend fun restoreLocked(existing: ActivityEntity) {
        settings = settingsRepo.current()
        val points = pointDao.forActivity(existing.id).map { it.toTrackPoint() }
        val now = time.nowMs()
        entity = existing
        resetCounters(now)
        lastAccepted = points.lastOrNull()
        val distance = TrackStats.distance(points)
        val moving = (TrackStats.movingTimeS(points) * 1000).roundToLong()
        lastSplitIndex = floor(distance / settings.unit.meters).toInt()
        val status = when {
            existing.status == ActivityStatus.PAUSED -> TrackingStatus.PAUSED
            existing.resumedAt == null -> TrackingStatus.AUTO_PAUSED
            else -> TrackingStatus.ACTIVE
        }
        val (goal, goalName) = goalFor(existing.type)
        _state.value = TrackingState(
            activityId = existing.id, type = existing.type, status = status, startedAt = existing.startTime,
            accumulatedMs = existing.durationS * 1000, resumedAt = existing.resumedAt, nowMs = now,
            distanceM = distance, movingMs = moving, segment = existing.segment,
            segments = points.groupBy { it.segment }.toSortedMap().values.map { seg -> seg.map { LatLngPoint(it.lat, it.lng) } },
            current = points.lastOrNull(),
            calories = Calories.estimate(existing.type, settings.weightKg.toDouble(), moving / 1000.0, distance),
            goalM = goal, goalHabitName = goalName, guide = loadGuide(existing.guideActivityId),
        )
        TrackingService.start(context)
        startTicker()
    }

    fun onLocation(location: Location) {
        scope.launch { handleLocation(location) }
    }

    private suspend fun handleLocation(loc: Location) = mutex.withLock {
        val s = _state.value
        val id = s.activityId ?: return@withLock
        val now = time.nowMs()
        lastFixAt = now
        val gps = when {
            loc.accuracy <= 10f -> GpsQuality.STRONG
            loc.accuracy <= 25f -> GpsQuality.WEAK
            else -> GpsQuality.SEARCHING
        }
        val point = TrackPoint(
            lat = loc.latitude, lng = loc.longitude,
            altitude = if (loc.hasAltitude()) loc.altitude else null,
            accuracy = loc.accuracy,
            speed = if (loc.hasSpeed()) loc.speed else null,
            timeMs = now,
            segment = s.segment,
        )
        updateSlowState(point, now)

        if (s.status == TrackingStatus.PAUSED) {
            _state.update { it.copy(gps = gps, current = point) }
            return@withLock
        }

        when (val result = filter.evaluate(lastAccepted, point)) {
            is FilterResult.Accept -> {
                pointDao.insert(
                    LocationPointEntity(
                        activityId = id, lat = point.lat, lng = point.lng, alt = point.altitude, accuracy = point.accuracy,
                        speed = point.speed, timestamp = point.timeMs, segment = point.segment,
                    ),
                )
                val previous = lastAccepted
                lastAccepted = point
                var movingAdd = 0L
                if (result.distanceM > 0 && previous != null && previous.segment == point.segment) {
                    val dt = point.timeMs - previous.timeMs
                    if (dt in 1..60_000) movingAdd = dt
                    lastMovementAt = now
                }
                var next = s
                // Moving again after an auto-pause restarts the clock.
                if (s.status == TrackingStatus.AUTO_PAUSED && result.distanceM > 0) {
                    next = next.copy(status = TrackingStatus.ACTIVE, resumedAt = now)
                    entity = entity?.copy(resumedAt = now)
                }
                val distance = next.distanceM + result.distanceM
                val moving = next.movingMs + movingAdd
                val segments = appendPoint(next.segments, point, startNew = previous == null || previous.segment != point.segment)
                next = next.copy(
                    distanceM = distance, movingMs = moving, segments = segments, current = point, gps = gps, nowMs = now,
                    calories = Calories.estimate(next.type, settings.weightKg.toDouble(), moving / 1000.0, distance),
                )
                _state.value = next
                announceSplitIfNeeded(next)
                entity = entity?.copy(distanceM = distance, movingTimeS = moving / 1000)?.also { activityDao.update(it) }
            }
            else -> _state.update { it.copy(gps = gps, current = point, nowMs = now) }
        }
    }

    private fun appendPoint(segments: List<List<LatLngPoint>>, p: TrackPoint, startNew: Boolean): List<List<LatLngPoint>> {
        val ll = LatLngPoint(p.lat, p.lng)
        if (startNew || segments.isEmpty()) return segments + listOf(listOf(ll))
        return segments.dropLast(1) + listOf(segments.last() + ll)
    }

    private fun announceSplitIfNeeded(s: TrackingState) {
        val unit = settings.unit
        val index = floor(s.distanceM / unit.meters).toInt()
        if (index <= lastSplitIndex) return
        val splitMs = s.elapsedMs - lastSplitElapsedMs
        lastSplitIndex = index
        lastSplitElapsedMs = s.elapsedMs
        if (!settings.voiceFeedback) return
        val (min, sec) = Format.paceParts(splitMs / 1000.0 / (unit.meters / 1000.0), unit)
        val res = if (unit == DistanceUnit.MILES) R.plurals.voice_split_mi else R.plurals.voice_split_km
        voice.speak(context.resources.getQuantityString(res, index, index, min, sec))
    }

    private fun updateSlowState(point: TrackPoint, now: Long) {
        val speed = point.speed ?: return
        slowSince = if (speed < SLOW_SPEED_MPS) (slowSince ?: now) else null
        updateLocationMode(now)
    }

    private fun updateLocationMode(now: Long) {
        val slow = slowSince?.let { now - it > SLOW_AFTER_MS } == true
        val saver = settings.batterySaverGps
        val mode = when {
            slow && saver -> LocationMode(10_000, highAccuracy = false)
            slow -> LocationMode(5_000, highAccuracy = true)
            saver -> LocationMode(4_000, highAccuracy = false)
            else -> LocationMode(NORMAL_INTERVAL_MS, highAccuracy = true)
        }
        if (mode != _locationMode.value) _locationMode.value = mode
    }

    suspend fun pause() = mutex.withLock { pauseLocked(TrackingStatus.PAUSED) }

    private suspend fun pauseLocked(newStatus: TrackingStatus) {
        val s = _state.value
        if (s.activityId == null || s.resumedAt == null) {
            // Already paused (e.g. auto-paused → manual pause): just switch status.
            if (s.activityId != null && newStatus == TrackingStatus.PAUSED && s.status != TrackingStatus.PAUSED) {
                _state.value = s.copy(status = TrackingStatus.PAUSED)
                entity = entity?.copy(status = ActivityStatus.PAUSED)?.also { activityDao.update(it) }
            }
            return
        }
        val now = time.nowMs()
        val accumulated = s.accumulatedMs + (now - s.resumedAt)
        _state.value = s.copy(status = newStatus, accumulatedMs = accumulated, resumedAt = null, nowMs = now)
        entity = entity?.copy(
            status = if (newStatus == TrackingStatus.PAUSED) ActivityStatus.PAUSED else ActivityStatus.ACTIVE,
            durationS = accumulated / 1000, resumedAt = null,
        )?.also { activityDao.update(it) }
    }

    suspend fun resume() = mutex.withLock {
        val s = _state.value
        if (s.activityId == null || s.status == TrackingStatus.ACTIVE) return@withLock
        val now = time.nowMs()
        // Manual resume starts a new segment so the paused gap isn't counted as distance.
        val segment = if (s.status == TrackingStatus.PAUSED) s.segment + 1 else s.segment
        _state.value = s.copy(status = TrackingStatus.ACTIVE, resumedAt = now, segment = segment, nowMs = now)
        lastMovementAt = now
        entity = entity?.copy(status = ActivityStatus.ACTIVE, resumedAt = now, segment = segment)?.also { activityDao.update(it) }
    }

    /** Finishes and saves the session. Returns null when nothing was tracking. */
    suspend fun finish(): FinishResult? {
        val id = mutex.withLock {
            val s = _state.value
            val id = s.activityId ?: return null
            pauseLocked(TrackingStatus.PAUSED)
            id
        }
        val result = processor.finalize(id)
        clear()
        result?.let { _finished.tryEmit(it) }
        return result
    }

    /** Throws the session away (e.g. too short to save). */
    suspend fun discard() {
        val id = mutex.withLock { _state.value.activityId } ?: return
        activityDao.delete(id)
        clear()
    }

    private suspend fun clear() = mutex.withLock {
        ticker?.cancel()
        ticker = null
        entity = null
        lastAccepted = null
        _state.value = TrackingState()
        TrackingService.stop(context)
    }

    private fun resetCounters(now: Long) {
        lastAccepted = null
        lastFixAt = 0L
        lastMovementAt = now
        slowSince = null
        lastSplitIndex = 0
        lastSplitElapsedMs = 0L
    }

    /** 1 Hz clock: drives the elapsed timer, auto-pause and the "GPS searching" state. */
    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(1_000)
                mutex.withLock {
                    val now = time.nowMs()
                    val s = _state.value
                    if (s.activityId == null) return@withLock
                    var next = s.copy(nowMs = now)
                    if (now - lastFixAt > SEARCHING_AFTER_MS) next = next.copy(gps = GpsQuality.SEARCHING)
                    _state.value = next
                    if (settings.autoPause && s.status == TrackingStatus.ACTIVE && now - lastMovementAt > AUTO_PAUSE_AFTER_MS) {
                        pauseLocked(TrackingStatus.AUTO_PAUSED)
                    }
                }
            }
        }
    }

    /** Remaining distance goal: an unfinished activity habit of this type, else the daily goal. */
    private suspend fun goalFor(type: ActivityType): Pair<Double?, String?> {
        val today = time.today()
        val todaysLogs = habits.logsFor(today)
        val habit = habits.activeHabits().firstOrNull {
            it.type == HabitType.ACTIVITY && it.linkedActivityType == type && it.activityGoalUnit == ActivityGoalUnit.METERS &&
                it.schedule.isScheduledOn(today) && todaysLogs[it.id]?.completed != true
        }
        if (habit != null) {
            val done = todaysLogs[habit.id]?.value ?: 0
            return (habit.target - done).toDouble().coerceAtLeast(0.0) to habit.name
        }
        val doneToday = activityDao.getCompletedBetween(time.startOfDayMs(today), time.endOfDayMs(today)).sumOf { it.distanceM }
        val remaining = settings.dailyGoalKm * 1000.0 - doneToday
        return (if (remaining > 0) remaining else null) to null
    }

    private suspend fun loadGuide(guideId: Long?): List<LatLngPoint> {
        if (guideId == null || guideId <= 0) return emptyList()
        return pointDao.forActivity(guideId).map { LatLngPoint(it.lat, it.lng) }
    }

    private companion object {
        const val NORMAL_INTERVAL_MS = 2_000L
        const val SLOW_SPEED_MPS = 1.0f
        const val SLOW_AFTER_MS = 20_000L
        const val AUTO_PAUSE_AFTER_MS = 12_000L
        const val SEARCHING_AFTER_MS = 10_000L
    }
}
