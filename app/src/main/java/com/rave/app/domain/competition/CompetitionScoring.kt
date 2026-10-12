package com.rave.app.domain.competition

import com.rave.app.domain.model.ActivitySummary
import com.rave.app.domain.model.ActivityType
import java.time.LocalDate

/** Why an activity was or wasn't counted. */
enum class Eligibility { OK, TOO_FAST, FLAGGED_AUTO_DETECTED }

/**
 * Basic anti-cheat. Only GPS-tracked activities exist in the app (there is no manual km
 * entry), activities with an unrealistic average speed are rejected, and auto-detected walks
 * still count but are flagged.
 */
object AntiCheat {
    const val MAX_RUN_KMH = 25.0
    const val MAX_WALK_KMH = 12.0

    fun check(a: ActivitySummary): Eligibility {
        if (a.movingTimeS > 0) {
            val kmh = (a.distanceM / 1000.0) / (a.movingTimeS / 3600.0)
            val max = if (a.type == ActivityType.RUN) MAX_RUN_KMH else MAX_WALK_KMH
            if (kmh > max) return Eligibility.TOO_FAST
        }
        return if (a.autoDetected) Eligibility.FLAGGED_AUTO_DETECTED else Eligibility.OK
    }
}

/** Day-level data used for habit-based competitions. */
data class HabitDay(val date: LocalDate, val completedHabitNames: List<String>)

data class ScoreResult(val value: Double, val flagged: Int, val reachedTargetAtMs: Long? = null)

/** Computes a participant's value from their own data within the competition window. */
object CompetitionScoring {

    fun score(
        competition: Competition,
        activities: List<ActivitySummary>,
        habitDays: List<HabitDay>,
        startDate: LocalDate,
        endDateInclusive: LocalDate,
    ): ScoreResult {
        val inWindow = activities.filter { it.startTimeMs >= competition.startAtMs && it.startTimeMs < competition.endAtMs }
        val eligible = inWindow.filter { AntiCheat.check(it) != Eligibility.TOO_FAST }
        val flagged = eligible.count { AntiCheat.check(it) == Eligibility.FLAGGED_AUTO_DETECTED }
        val days = habitDays.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDateInclusive) }

        return when (competition.type) {
            CompetitionType.DISTANCE -> {
                val counted = eligible.filter { competition.activityFilter == null || it.type == competition.activityFilter }
                ScoreResult(counted.sumOf { it.distanceM } / 1000.0, flagged)
            }
            CompetitionType.TARGET_RACE -> {
                val counted = eligible
                    .filter { competition.activityFilter == null || it.type == competition.activityFilter }
                    .sortedBy { it.startTimeMs }
                var km = 0.0
                var reachedAt: Long? = null
                for (a in counted) {
                    km += a.distanceM / 1000.0
                    if (reachedAt == null && km >= competition.targetKm) {
                        reachedAt = a.startTimeMs + a.movingTimeS * 1000
                    }
                }
                ScoreResult(km, flagged, reachedAt)
            }
            CompetitionType.ACTIVE_DAYS -> {
                val activityDays = eligible.map { it.startDateIn(startDate, endDateInclusive) }.filterNotNull().toSet()
                val habitActive = days.filter { it.completedHabitNames.isNotEmpty() }.map { it.date }.toSet()
                ScoreResult((activityDays + habitActive).size.toDouble(), flagged)
            }
            CompetitionType.HABITS_COMPLETED -> {
                val names = competition.habitNames.map { it.trim().lowercase() }.toSet()
                val count = days.sumOf { day ->
                    day.completedHabitNames.count { names.isEmpty() || it.trim().lowercase() in names }
                }
                ScoreResult(count.toDouble(), flagged)
            }
            CompetitionType.LONGEST_STREAK -> {
                val doneDates = days.filter { it.completedHabitNames.isNotEmpty() }.map { it.date }.toSet()
                ScoreResult(longestRun(doneDates, startDate, endDateInclusive).toDouble(), flagged)
            }
        }
    }

    /** Longest run of consecutive dates in [dates] between [from] and [to]. */
    fun longestRun(dates: Set<LocalDate>, from: LocalDate, to: LocalDate): Int {
        var best = 0
        var run = 0
        var d = from
        while (!d.isAfter(to)) {
            if (d in dates) {
                run++
                best = maxOf(best, run)
            } else {
                run = 0
            }
            d = d.plusDays(1)
        }
        return best
    }

    private fun ActivitySummary.startDateIn(from: LocalDate, to: LocalDate): LocalDate? {
        val date = java.time.Instant.ofEpochMilli(startTimeMs).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        return date.takeIf { !it.isBefore(from) && !it.isAfter(to) }
    }
}
