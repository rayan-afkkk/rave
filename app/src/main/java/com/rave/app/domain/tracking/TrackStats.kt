package com.rave.app.domain.tracking

import com.rave.app.domain.model.Split
import com.rave.app.domain.model.TrackPoint

/**
 * Derived statistics over a list of accepted [TrackPoint]s (time-ordered).
 * Distance and time never accumulate across a segment boundary (pause).
 */
object TrackStats {

    /** Total distance in metres. */
    fun distance(points: List<TrackPoint>): Double {
        var total = 0.0
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            if (a.segment == b.segment) total += GeoMath.haversine(a.lat, a.lng, b.lat, b.lng)
        }
        return total
    }

    /**
     * Time spent moving: sum of intervals inside a segment where speed is at least [minSpeedMps].
     * Intervals longer than [maxGapS] (signal lost) are ignored.
     */
    fun movingTimeS(points: List<TrackPoint>, minSpeedMps: Double = 0.4, maxGapS: Double = 60.0): Double {
        var total = 0.0
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            if (a.segment != b.segment) continue
            val dt = (b.timeMs - a.timeMs) / 1000.0
            if (dt <= 0 || dt > maxGapS) continue
            val d = GeoMath.haversine(a.lat, a.lng, b.lat, b.lng)
            if (d / dt >= minSpeedMps) total += dt
        }
        return total
    }

    /** Average pace in seconds per km, or null if no distance. */
    fun paceSecondsPerKm(movingTimeS: Double, distanceM: Double): Double? =
        if (distanceM < 1.0) null else movingTimeS / (distanceM / 1000.0)

    /**
     * Elevation gain with a hysteresis [thresholdM] so GPS altitude noise isn't summed.
     * Returns 0 when no altitude data is available.
     */
    fun elevationGain(points: List<TrackPoint>, thresholdM: Double = 3.0): Double {
        val alts = points.mapNotNull { it.altitude }
        if (alts.size < 2) return 0.0
        var gain = 0.0
        var ref = alts.first()
        for (alt in alts.drop(1)) {
            val diff = alt - ref
            if (diff >= thresholdM) {
                gain += diff
                ref = alt
            } else if (diff <= -thresholdM) {
                ref = alt
            }
        }
        return gain
    }

    /** Cumulative (distance, activeTime) per point, frozen across segment boundaries. */
    private fun cumulative(points: List<TrackPoint>): Pair<DoubleArray, DoubleArray> {
        val dist = DoubleArray(points.size)
        val time = DoubleArray(points.size)
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            if (a.segment == b.segment) {
                dist[i] = dist[i - 1] + GeoMath.haversine(a.lat, a.lng, b.lat, b.lng)
                time[i] = time[i - 1] + (b.timeMs - a.timeMs) / 1000.0
            } else {
                dist[i] = dist[i - 1]
                time[i] = time[i - 1]
            }
        }
        return dist to time
    }

    /** Time at cumulative distance [target], linearly interpolated. */
    private fun timeAt(dist: DoubleArray, time: DoubleArray, target: Double): Double {
        if (target <= 0.0) return 0.0
        var i = 1
        while (i < dist.size && dist[i] < target) i++
        if (i >= dist.size) return time.last()
        val span = dist[i] - dist[i - 1]
        if (span <= 0.0) return time[i]
        val f = (target - dist[i - 1]) / span
        return time[i - 1] + f * (time[i] - time[i - 1])
    }

    /** Splits every [unitMeters] (1000 = km). The last split may be partial. */
    fun splits(points: List<TrackPoint>, unitMeters: Double = 1000.0): List<Split> {
        if (points.size < 2) return emptyList()
        val (dist, time) = cumulative(points)
        val total = dist.last()
        if (total <= 0.0) return emptyList()
        val result = mutableListOf<Split>()
        var start = 0.0
        var startTime = 0.0
        var index = 1
        while (start < total) {
            val end = minOf(start + unitMeters, total)
            val endTime = timeAt(dist, time, end)
            // Skip a meaningless tail shorter than 10 m.
            if (end - start >= 10.0 || result.isEmpty()) {
                result += Split(index, end - start, endTime - startTime)
            }
            start = end
            startTime = endTime
            index++
        }
        return result
    }

    /**
     * Fastest time in seconds to cover [windowMeters] anywhere in the track
     * (e.g. fastest 1K inside a 5K run). Null if the track is shorter than the window.
     */
    fun fastestWindowS(points: List<TrackPoint>, windowMeters: Double): Double? {
        if (points.size < 2) return null
        val (dist, time) = cumulative(points)
        if (dist.last() < windowMeters) return null
        var best = Double.MAX_VALUE
        // For each point as the window end, find the interpolated window start.
        for (j in points.indices) {
            val startDist = dist[j] - windowMeters
            if (startDist < 0) continue
            val elapsed = time[j] - timeAt(dist, time, startDist)
            if (elapsed >= 0.0 && elapsed < best) best = elapsed
        }
        return best.takeIf { it != Double.MAX_VALUE }
    }
}
