package com.rave.app.domain.model

/** GPS-tracked activity kinds. */
enum class ActivityType { WALK, RUN }

/** Lifecycle of an activity row. */
enum class ActivityStatus {
    /** Currently being tracked. */
    ACTIVE,
    /** Tracking paused by the user (or auto-pause). */
    PAUSED,
    /** Background walk detection is recording points; not yet confirmed by the user. */
    DETECTING,
    /** Finished and saved. */
    COMPLETED,
}

enum class PbTag { FASTEST_1K, FASTEST_5K, LONGEST_DISTANCE }

enum class DistanceUnit(val meters: Double) {
    KM(1000.0),
    MILES(1609.344),
}

/** A single filtered GPS fix. [segment] increments after every pause so gaps aren't counted. */
data class TrackPoint(
    val lat: Double,
    val lng: Double,
    val altitude: Double? = null,
    val accuracy: Float = 5f,
    val speed: Float? = null,
    val timeMs: Long,
    val segment: Int = 0,
)

data class LatLngPoint(val lat: Double, val lng: Double)

/** One kilometre (or mile) split. */
data class Split(
    val index: Int,
    val distanceM: Double,
    val durationS: Double,
) {
    /** Seconds per full unit (pro-rated for the final partial split). */
    fun paceSecondsPer(unitMeters: Double): Double =
        if (distanceM <= 0.0) 0.0 else durationS / distanceM * unitMeters
}

/** Lightweight summary used by competitions, insights and anti-cheat. */
data class ActivitySummary(
    val id: Long,
    val type: ActivityType,
    val startTimeMs: Long,
    val distanceM: Double,
    val movingTimeS: Long,
    val autoDetected: Boolean,
)
