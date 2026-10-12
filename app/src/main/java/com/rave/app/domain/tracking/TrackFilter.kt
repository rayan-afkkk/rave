package com.rave.app.domain.tracking

import com.rave.app.domain.model.TrackPoint

/** Outcome of running a new GPS fix through [TrackFilter]. */
sealed interface FilterResult {
    /** Keep the point; [distanceM] is the distance added since the previous accepted point. */
    data class Accept(val distanceM: Double) : FilterResult
    /** Too close to the previous point to count (GPS jitter while standing still). */
    data object Stationary : FilterResult
    data object PoorAccuracy : FilterResult
    data object ImpossibleJump : FilterResult
}

/**
 * Rejects bad GPS fixes: low accuracy (> [maxAccuracyM]), impossible speed jumps
 * (> [maxSpeedMps] implied between fixes) and sub-[minMoveM] jitter.
 */
class TrackFilter(
    private val maxAccuracyM: Float = 25f,
    private val maxSpeedMps: Double = 12.0,
    private val minMoveM: Double = 3.0,
) {
    fun evaluate(last: TrackPoint?, candidate: TrackPoint): FilterResult {
        if (candidate.accuracy > maxAccuracyM) return FilterResult.PoorAccuracy
        // First point, or first point after a pause: nothing to compare against.
        if (last == null || last.segment != candidate.segment) return FilterResult.Accept(0.0)

        val d = GeoMath.haversine(last.lat, last.lng, candidate.lat, candidate.lng)
        val dtS = (candidate.timeMs - last.timeMs) / 1000.0
        if (dtS <= 0) return FilterResult.Stationary
        if (d / dtS > maxSpeedMps) return FilterResult.ImpossibleJump
        // Allow for accuracy noise: require movement beyond the larger of minMove and half the accuracy.
        val threshold = maxOf(minMoveM, candidate.accuracy / 2.0)
        if (d < threshold) return FilterResult.Stationary
        return FilterResult.Accept(d)
    }
}
