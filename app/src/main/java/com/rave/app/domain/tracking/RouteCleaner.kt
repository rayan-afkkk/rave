package com.rave.app.domain.tracking

import com.rave.app.domain.model.LatLngPoint
import com.rave.app.domain.model.TrackPoint
import kotlin.math.hypot

/**
 * Turns raw GPS fixes into a clean line for drawing without a map (share cards):
 * - one line per recording segment, so pauses don't become straight jumps,
 * - fixes worse than [MAX_ACCURACY_M] dropped (the same limit the tracker records with, so the
 *   card shows every part of the route the summary map shows),
 * - jitter smoothed with a small moving average (ends kept exact),
 * - tiny zig-zags removed with a tolerance relative to the route's size.
 *
 * A short walk drawn edge to edge on a card magnifies a few metres of GPS noise into a
 * scribble; this keeps the shape you actually walked.
 */
object RouteCleaner {
    const val MAX_ACCURACY_M = 25f
    private const val SMOOTH_RADIUS = 2 // 5-point window

    fun clean(points: List<TrackPoint>): List<List<LatLngPoint>> {
        if (points.size < 2) return emptyList()
        val segments = points.groupBy { it.segment }.values.map { seg ->
            val accurate = seg.filter { it.accuracy <= MAX_ACCURACY_M }
            (if (accurate.size >= 2) accurate else seg).map { LatLngPoint(it.lat, it.lng) }
        }
        val extent = extentMeters(segments.flatten())
        val tolerance = (extent * 0.01).coerceIn(1.0, 8.0)
        return segments
            .map { Polyline.simplify(smooth(it), tolerance) }
            .filter { it.size >= 2 }
    }

    /** Centred moving average; the first and last points stay where they were. */
    fun smooth(points: List<LatLngPoint>, radius: Int = SMOOTH_RADIUS): List<LatLngPoint> {
        if (points.size < 3) return points
        return points.indices.map { i ->
            if (i == 0 || i == points.lastIndex) return@map points[i]
            val r = minOf(radius, i, points.lastIndex - i)
            var lat = 0.0
            var lng = 0.0
            for (j in i - r..i + r) {
                lat += points[j].lat
                lng += points[j].lng
            }
            val n = 2 * r + 1
            LatLngPoint(lat / n, lng / n)
        }
    }

    private fun extentMeters(points: List<LatLngPoint>): Double {
        if (points.size < 2) return 0.0
        val ref = points.first()
        val xy = points.map { GeoMath.toLocalMeters(it.lat, it.lng, ref.lat, ref.lng) }
        val w = xy.maxOf { it.first } - xy.minOf { it.first }
        val h = xy.maxOf { it.second } - xy.minOf { it.second }
        return hypot(w, h)
    }
}
