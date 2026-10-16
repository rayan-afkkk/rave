package com.rave.app.domain.notify

import java.time.LocalTime
import kotlin.math.ceil

/** Evening nudge when today's steps are behind the goal. */
data class StepNudge(val stepsShort: Int, val walkMinutes: Int, val closesGap: Boolean)

object StepNudges {
    /** Brisk walking is roughly 100 steps a minute. */
    const val STEPS_PER_MINUTE = 100
    private const val MIN_SHORT = 300
    private const val MAX_MINUTES = 45
    /** Minutes after sunset: time to pray Maghrib before heading out. */
    const val AFTER_SUNSET_MIN = 20L
    val FALLBACK_TIME: LocalTime = LocalTime.of(19, 30)
    private val LATEST: LocalTime = LocalTime.of(21, 30)

    /** Null when the goal is (nearly) met; otherwise how far off and how long a walk closes the gap. */
    fun evaluate(steps: Int, goal: Int): StepNudge? {
        val short = goal - steps
        if (goal <= 0 || short < MIN_SHORT) return null
        val minutes = (ceil(short / STEPS_PER_MINUTE / 5.0) * 5).toInt().coerceIn(5, MAX_MINUTES)
        return StepNudge(short, minutes, closesGap = minutes * STEPS_PER_MINUTE >= short)
    }

    /** When to nudge: a little after sunset (Maghrib), else 7:30 pm; never later than 9:30 pm. */
    fun nudgeTime(sunset: LocalTime?): LocalTime {
        val t = sunset?.plusMinutes(AFTER_SUNSET_MIN) ?: FALLBACK_TIME
        return if (t.isAfter(LATEST)) LATEST else t
    }
}
