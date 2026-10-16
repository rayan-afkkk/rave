package com.rave.app.domain.notify

import java.time.LocalDate
import kotlin.math.roundToInt

/** A week's summary for the Sunday report. */
data class WeeklyReport(
    val weekStart: LocalDate,
    val steps: Int,
    val prevSteps: Int,
    val distanceM: Double,
    val prevDistanceM: Double,
    val activities: Int,
    val habitsDone: Int,
    val habitsDue: Int,
    val bestDaySteps: Int,
    val streak: Int,
) {
    val distanceChange: Int? get() = WeeklyReports.changePercent(distanceM, prevDistanceM)
    val stepsChange: Int? get() = WeeklyReports.changePercent(steps.toDouble(), prevSteps.toDouble())
    val habitRate: Int? get() = if (habitsDue <= 0) null else (habitsDone * 100.0 / habitsDue).roundToInt()
    /** Lead with distance when the user walked/ran with GPS, otherwise steps. */
    val leadsWithDistance: Boolean get() = distanceM >= 500
    val isEmpty: Boolean get() = steps == 0 && distanceM < 1 && activities == 0 && habitsDone == 0
}

object WeeklyReports {
    /** Percent change vs last week, or null when there's nothing to compare with. */
    fun changePercent(now: Double, prev: Double): Int? = if (prev <= 0) null else ((now - prev) / prev * 100).roundToInt()
}
