package com.rave.app.domain.wrapped

import java.time.LocalDate
import java.time.Month

data class WrappedActivity(val date: LocalDate, val distanceM: Double, val durationS: Long, val name: String)
data class WrappedHabitDone(val habitName: String, val date: LocalDate)
data class WrappedWorkout(val date: LocalDate, val volumeKg: Double)

enum class WrappedPersona { ROAD_WARRIOR, IRON_LIFTER, HABIT_HERO, STEP_MACHINE, ALL_ROUNDER, FRESH_START }

/** A Wrapped period: a whole [year], or one [month] (1-12) of it. */
data class WrappedPeriod(val year: Int, val month: Int? = null) {
    val isYear: Boolean get() = month == null
    val from: LocalDate get() = if (month == null) LocalDate.of(year, 1, 1) else LocalDate.of(year, month, 1)
    val to: LocalDate get() = if (month == null) LocalDate.of(year, 12, 31) else from.withDayOfMonth(from.lengthOfMonth())
    operator fun contains(d: LocalDate): Boolean = !d.isBefore(from) && !d.isAfter(to)
}

data class WrappedSummary(
    val year: Int,
    /** Set for a monthly Wrapped. */
    val month: Int? = null,
    val totalSteps: Long,
    val bestStepDay: Pair<LocalDate, Int>?,
    val totalKm: Double,
    val activities: Int,
    val activeHours: Double,
    val longest: WrappedActivity?,
    val habitCompletions: Int,
    val topHabit: Pair<String, Int>?,
    val bestStreak: Int,
    val workouts: Int,
    val liftedKg: Double,
    val peakMonth: Month?,
    val persona: WrappedPersona,
) {
    val isEmpty: Boolean get() = totalSteps == 0L && activities == 0 && habitCompletions == 0 && workouts == 0
}

/** "Rave Wrapped": the year in numbers, computed from local data only. */
object WrappedCalculator {

    fun summarize(
        period: WrappedPeriod,
        dailySteps: List<Pair<LocalDate, Int>>,
        activities: List<WrappedActivity>,
        habitsDone: List<WrappedHabitDone>,
        workouts: List<WrappedWorkout>,
        bestStreak: Int,
    ): WrappedSummary {
        val steps = dailySteps.filter { it.first in period }
        val acts = activities.filter { it.date in period }
        val done = habitsDone.filter { it.date in period }
        val lifts = workouts.filter { it.date in period }

        val totalKm = acts.sumOf { it.distanceM } / 1000.0
        val topHabit = done.groupingBy { it.habitName }.eachCount().maxByOrNull { it.value }?.toPair()

        // Peak month: weighted mix so walkers, lifters and habit builders all get a fair answer.
        val score = mutableMapOf<Month, Double>()
        steps.forEach { (d, s) -> score.merge(d.month, s / 1000.0, Double::plus) }
        acts.forEach { score.merge(it.date.month, it.distanceM / 1000.0 * 2, Double::plus) }
        done.forEach { score.merge(it.date.month, 1.0, Double::plus) }
        lifts.forEach { score.merge(it.date.month, 5.0, Double::plus) }
        val peak = if (period.isYear) score.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key else null

        val totalSteps = steps.sumOf { it.second.toLong() }
        val k = if (period.isYear) 1.0 else 1.0 / 12   // a month needs a twelfth of the yearly totals
        val persona = when {
            totalSteps == 0L && acts.isEmpty() && done.isEmpty() && lifts.isEmpty() -> WrappedPersona.FRESH_START
            lifts.size >= 20 * k && lifts.size >= acts.size -> WrappedPersona.IRON_LIFTER
            totalKm >= 100 * k -> WrappedPersona.ROAD_WARRIOR
            done.size >= 200 * k -> WrappedPersona.HABIT_HERO
            totalSteps >= 1_000_000 * k -> WrappedPersona.STEP_MACHINE
            else -> WrappedPersona.ALL_ROUNDER
        }

        return WrappedSummary(
            year = period.year,
            month = period.month,
            totalSteps = totalSteps,
            bestStepDay = steps.maxByOrNull { it.second }?.takeIf { it.second > 0 },
            totalKm = totalKm,
            activities = acts.size,
            activeHours = (acts.sumOf { it.durationS }) / 3600.0,
            longest = acts.maxByOrNull { it.distanceM },
            habitCompletions = done.size,
            topHabit = topHabit,
            bestStreak = bestStreak,
            workouts = lifts.size,
            liftedKg = lifts.sumOf { it.volumeKg },
            peakMonth = peak,
            persona = persona,
        )
    }

    /**
     * Wrapped periods available on [today], yearly first:
     * - monthly: from the last day of a month until the 7th of the next one,
     * - yearly: from 31 December until 14 January.
     */
    fun available(today: LocalDate): List<WrappedPeriod> = buildList {
        if (today.monthValue == 12 && today.dayOfMonth == 31) add(WrappedPeriod(today.year))
        if (today.monthValue == 1 && today.dayOfMonth <= 14) add(WrappedPeriod(today.year - 1))
        if (today.dayOfMonth == today.lengthOfMonth()) add(WrappedPeriod(today.year, today.monthValue))
        else if (today.dayOfMonth <= 7) today.minusMonths(1).let { add(WrappedPeriod(it.year, it.monthValue)) }
    }
}
