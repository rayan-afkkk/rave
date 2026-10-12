package com.rave.app.domain.tracking

import com.rave.app.domain.model.ActivityType
import kotlin.math.roundToInt

/** MET-based calorie estimate: kcal = MET × weight(kg) × hours. */
object Calories {

    /** MET value for [type], nudged by pace (seconds per km) when known. */
    fun met(type: ActivityType, paceSPerKm: Double?): Double = when (type) {
        ActivityType.WALK -> when {
            paceSPerKm == null -> 3.5
            paceSPerKm < 9 * 60 -> 5.0   // brisk, > 6.7 km/h
            paceSPerKm < 11 * 60 -> 4.3  // ~5.5–6.7 km/h
            paceSPerKm > 15 * 60 -> 2.8  // stroll, < 4 km/h
            else -> 3.5
        }
        ActivityType.RUN -> when {
            paceSPerKm == null -> 9.8
            paceSPerKm < 4 * 60 -> 14.5
            paceSPerKm < 5 * 60 -> 11.5
            paceSPerKm > 7 * 60 -> 8.3
            else -> 9.8
        }
    }

    fun estimate(type: ActivityType, weightKg: Double, movingTimeS: Double, distanceM: Double): Int {
        if (movingTimeS <= 0 || weightKg <= 0) return 0
        val pace = TrackStats.paceSecondsPerKm(movingTimeS, distanceM)
        return (met(type, pace) * weightKg * (movingTimeS / 3600.0)).roundToInt()
    }
}
