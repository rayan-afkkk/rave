package com.rave.app.domain.util

/** Part of the day, used for greetings ("Morning, Rayan") and default activity names ("Morning walk"). */
enum class DayPeriod { MORNING, AFTERNOON, EVENING, NIGHT }

/**
 * Maps an hour of day (0–23) to a [DayPeriod].
 * 05:00–11:59 morning, 12:00–16:59 afternoon, 17:00–21:59 evening, otherwise night.
 */
fun dayPeriodFor(hour: Int): DayPeriod {
    require(hour in 0..23) { "hour must be 0..23, was $hour" }
    return when (hour) {
        in 5..11 -> DayPeriod.MORNING
        in 12..16 -> DayPeriod.AFTERNOON
        in 17..21 -> DayPeriod.EVENING
        else -> DayPeriod.NIGHT
    }
}
