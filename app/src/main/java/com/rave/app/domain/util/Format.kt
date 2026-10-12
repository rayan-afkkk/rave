package com.rave.app.domain.util

import com.rave.app.domain.model.DistanceUnit
import java.util.Locale
import kotlin.math.roundToLong

/** Locale-stable number formatting for stats. Unit labels come from string resources in the UI. */
object Format {

    /** "3.20" (km or miles depending on [unit]). */
    fun distance(meters: Double, unit: DistanceUnit, decimals: Int = 2): String =
        String.format(Locale.US, "%.${decimals}f", meters / unit.meters)

    /** "12:34" or "1:02:03". */
    fun duration(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "%d:%02d", m, sec)
    }

    /** Pace like "6:10" per km/mile, or "–:––" when unknown. */
    fun pace(secondsPerKm: Double?, unit: DistanceUnit): String {
        if (secondsPerKm == null || secondsPerKm <= 0 || secondsPerKm.isInfinite() || secondsPerKm > 60 * 60) return "–:––"
        val perUnit = (secondsPerKm * unit.meters / 1000.0).roundToLong()
        return String.format(Locale.US, "%d:%02d", perUnit / 60, perUnit % 60)
    }

    /** Minutes + seconds parts of a pace, for speech ("6 minutes 10"). */
    fun paceParts(secondsPerKm: Double, unit: DistanceUnit): Pair<Long, Long> {
        val perUnit = (secondsPerKm * unit.meters / 1000.0).roundToLong()
        return perUnit / 60 to perUnit % 60
    }
}
