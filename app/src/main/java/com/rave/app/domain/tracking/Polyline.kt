package com.rave.app.domain.tracking

import com.rave.app.domain.model.LatLngPoint
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sqrt

/** Google encoded-polyline format + Douglas–Peucker simplification. */
object Polyline {

    fun encode(points: List<LatLngPoint>): String {
        val sb = StringBuilder()
        var prevLat = 0L
        var prevLng = 0L
        for (p in points) {
            val lat = (p.lat * 1e5).roundToLong()
            val lng = (p.lng * 1e5).roundToLong()
            encodeValue(lat - prevLat, sb)
            encodeValue(lng - prevLng, sb)
            prevLat = lat
            prevLng = lng
        }
        return sb.toString()
    }

    private fun encodeValue(value: Long, sb: StringBuilder) {
        var v = if (value < 0) (value shl 1).inv() else value shl 1
        while (v >= 0x20) {
            sb.append(((0x20 or (v and 0x1f).toInt()) + 63).toChar())
            v = v shr 5
        }
        sb.append((v + 63).toInt().toChar())
    }

    fun decode(encoded: String): List<LatLngPoint> {
        val result = mutableListOf<LatLngPoint>()
        var index = 0
        var lat = 0L
        var lng = 0L
        while (index < encoded.length) {
            val (dLat, i1) = decodeValue(encoded, index)
            val (dLng, i2) = decodeValue(encoded, i1)
            index = i2
            lat += dLat
            lng += dLng
            result += LatLngPoint(lat / 1e5, lng / 1e5)
        }
        return result
    }

    private fun decodeValue(s: String, start: Int): Pair<Long, Int> {
        var result = 0L
        var shift = 0
        var i = start
        var b: Int
        do {
            b = s[i++].code - 63
            result = result or ((b and 0x1f).toLong() shl shift)
            shift += 5
        } while (b >= 0x20 && i < s.length)
        val value = if (result and 1L != 0L) (result shr 1).inv() else result shr 1
        return value to i
    }

    /** Douglas–Peucker with a tolerance in metres. Keeps first and last points. */
    fun simplify(points: List<LatLngPoint>, toleranceM: Double = 8.0): List<LatLngPoint> {
        if (points.size < 3) return points
        val ref = points.first()
        val xy = points.map { GeoMath.toLocalMeters(it.lat, it.lng, ref.lat, ref.lng) }
        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.lastIndex] = true
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(0 to points.lastIndex)
        while (stack.isNotEmpty()) {
            val (s, e) = stack.removeLast()
            var maxD = 0.0
            var idx = -1
            for (i in s + 1 until e) {
                val d = perpendicular(xy[i], xy[s], xy[e])
                if (d > maxD) {
                    maxD = d
                    idx = i
                }
            }
            if (idx != -1 && maxD > toleranceM) {
                keep[idx] = true
                stack.addLast(s to idx)
                stack.addLast(idx to e)
            }
        }
        return points.filterIndexed { i, _ -> keep[i] }
    }

    private fun perpendicular(p: Pair<Double, Double>, a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val dx = b.first - a.first
        val dy = b.second - a.second
        val len = sqrt(dx * dx + dy * dy)
        if (len == 0.0) return sqrt((p.first - a.first).let { it * it } + (p.second - a.second).let { it * it })
        return abs(dy * p.first - dx * p.second + b.first * a.second - b.second * a.first) / len
    }
}
