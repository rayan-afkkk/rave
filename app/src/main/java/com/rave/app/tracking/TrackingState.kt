package com.rave.app.tracking

import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.LatLngPoint
import com.rave.app.domain.model.TrackPoint

enum class TrackingStatus { IDLE, ACTIVE, PAUSED, AUTO_PAUSED }

enum class GpsQuality { STRONG, WEAK, SEARCHING }

/** How the service should request location updates. */
data class LocationMode(val intervalMs: Long, val highAccuracy: Boolean)

/** Everything the live tracking UI and notification need. */
data class TrackingState(
    val activityId: Long? = null,
    val type: ActivityType = ActivityType.WALK,
    val status: TrackingStatus = TrackingStatus.IDLE,
    val startedAt: Long = 0,
    /** Active time accumulated before the current run of [resumedAt]. */
    val accumulatedMs: Long = 0,
    /** Non-null while the clock is running. */
    val resumedAt: Long? = null,
    val nowMs: Long = 0,
    val distanceM: Double = 0.0,
    val movingMs: Long = 0,
    val calories: Int = 0,
    val segment: Int = 0,
    /** Route drawn on the map, one list per segment (pauses leave gaps). */
    val segments: List<List<LatLngPoint>> = emptyList(),
    val current: TrackPoint? = null,
    val gps: GpsQuality = GpsQuality.SEARCHING,
    /** Distance goal for this session (remaining habit goal or daily goal), metres. */
    val goalM: Double? = null,
    val goalHabitName: String? = null,
    /** Favourite route shown as a faint guide line. */
    val guide: List<LatLngPoint> = emptyList(),
) {
    val isTracking: Boolean get() = activityId != null
    val elapsedMs: Long get() = accumulatedMs + (resumedAt?.let { (nowMs - it).coerceAtLeast(0) } ?: 0)
    val start: LatLngPoint? get() = segments.firstOrNull()?.firstOrNull()

    /** Average moving pace, seconds per km. */
    val paceSPerKm: Double? get() = if (distanceM < 20) null else (movingMs / 1000.0) / (distanceM / 1000.0)
}
