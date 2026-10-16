package com.rave.app.domain.notify

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** Sunset time (≈ Maghrib) from the "Almanac for Computers" sunrise/sunset algorithm, accurate to a couple of minutes. */
object SunTimes {
    private const val ZENITH = 90.833 // official sunset: sun's upper edge on the horizon, with refraction

    /** Local sunset for [date] at the point, or null where the sun doesn't set (polar days/nights). */
    fun sunset(date: LocalDate, lat: Double, lng: Double, zone: ZoneId): LocalTime? {
        val n = date.dayOfYear
        val lngHour = lng / 15.0
        val t = n + (18 - lngHour) / 24.0
        val m = 0.9856 * t - 3.289
        val l = norm360(m + 1.916 * sinD(m) + 0.020 * sinD(2 * m) + 282.634)
        var ra = norm360(atanD(0.91764 * tanD(l)))
        ra += floor(l / 90) * 90 - floor(ra / 90) * 90
        ra /= 15.0
        val sinDec = 0.39782 * sinD(l)
        val cosDec = cos(asin(sinDec))
        val cosH = (cosD(ZENITH) - sinDec * sinD(lat)) / (cosDec * cosD(lat))
        if (cosH > 1 || cosH < -1) return null
        val h = Math.toDegrees(acos(cosH)) / 15.0
        val localMean = h + ra - 0.06571 * t - 6.622
        val ut = ((localMean - lngHour) % 24 + 24) % 24
        val offsetH = zone.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
        val local = ((ut + offsetH) % 24 + 24) % 24
        val totalMinutes = Math.round(local * 60).toInt() % (24 * 60)
        return LocalTime.of(totalMinutes / 60, totalMinutes % 60)
    }

    private fun norm360(x: Double) = ((x % 360) + 360) % 360
    private fun sinD(d: Double) = sin(Math.toRadians(d))
    private fun cosD(d: Double) = cos(Math.toRadians(d))
    private fun tanD(d: Double) = tan(Math.toRadians(d))
    private fun atanD(x: Double) = Math.toDegrees(atan(x))
}
