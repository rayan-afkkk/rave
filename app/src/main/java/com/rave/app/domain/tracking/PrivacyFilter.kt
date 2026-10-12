package com.rave.app.domain.tracking

import com.rave.app.domain.model.LatLngPoint

/** A circular area (home, work…) whose points are never shown on shared routes. */
data class PrivacyArea(val lat: Double, val lng: Double, val radiusM: Double)

/**
 * Applies privacy rules before a route leaves the device or is drawn on a share card:
 * - trims the first and last [trimMeters] of the route (if [trimEnds]),
 * - removes every point inside a [PrivacyArea].
 *
 * Returns visible segments; a gap is left wherever points were removed, so no straight
 * line is drawn across a hidden area.
 */
object PrivacyFilter {
    const val DEFAULT_TRIM_M = 200.0

    fun apply(
        points: List<LatLngPoint>,
        zones: List<PrivacyArea>,
        trimEnds: Boolean = true,
        trimMeters: Double = DEFAULT_TRIM_M,
    ): List<List<LatLngPoint>> {
        if (points.size < 2) return emptyList()
        var visible = points
        if (trimEnds) {
            val cumulative = DoubleArray(points.size)
            for (i in 1 until points.size) {
                cumulative[i] = cumulative[i - 1] + GeoMath.haversine(
                    points[i - 1].lat, points[i - 1].lng, points[i].lat, points[i].lng,
                )
            }
            val total = cumulative.last()
            // Route too short to show anything once both ends are hidden.
            if (total <= trimMeters * 2) return emptyList()
            visible = points.filterIndexed { i, _ ->
                cumulative[i] >= trimMeters && cumulative[i] <= total - trimMeters
            }
        }

        val segments = mutableListOf<List<LatLngPoint>>()
        var current = mutableListOf<LatLngPoint>()
        for (p in visible) {
            val hidden = zones.any { GeoMath.haversine(p.lat, p.lng, it.lat, it.lng) <= it.radiusM }
            if (hidden) {
                if (current.size >= 2) segments += current
                current = mutableListOf()
            } else {
                current += p
            }
        }
        if (current.size >= 2) segments += current
        return segments
    }
}
