package com.rave.app.domain.wrapped

import java.time.LocalDate
import java.time.Month

data class WrappedActivity(val date: LocalDate, val distanceM: Double, val durationS: Long, val name: String)
data class WrappedHabitDone(val habitName: String, val date: LocalDate)
data class WrappedWorkout(val date: LocalDate, val volumeKg: Double)

enum class WrappedPersona { ROAD_WARRIOR, IRON_LIFTER, HABIT_HERO, STEP_MACHINE, ALL_ROUNDER, FRESH_START }

data class WrappedSummary(
    val year: Int,
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
        year: Int,
        dailySteps: List<Pair<LocalDate, Int>>,
        activities: List<WrappedActivity>,
        habitsDone: List<WrappedHabitDone>,
        workouts: List<WrappedWorkout>,
        bestStreak: Int,
    ): WrappedSummary {
        val steps = dailySteps.filter { it.first.year == year }
        val acts = activities.filter { it.date.year == year }
        val done = habitsDone.filter { it.date.year == year }
        val lifts = workouts.filter { it.date.year == year }

        val totalKm = acts.sumOf { it.distanceM } / 1000.0
        val topHabit = done.groupingBy { it.habitName }.eachCount().maxByOrNull { it.value }?.toPair()

        // Peak month: weighted mix so walkers, lifters and habit builders all get a fair answer.
        val score = mutableMapOf<Month, Double>()
        steps.forEach { (d, s) -> score.merge(d.month, s / 1000.0, Double::plus) }
        acts.forEach { score.merge(it.date.month, it.distanceM / 1000.0 * 2, Double::plus) }
        done.forEach { score.merge(it.date.month, 1.0, Double::plus) }
        lifts.forEach { score.merge(it.date.month, 5.0, Double::plus) }
        val peak = score.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key

        val totalSteps = steps.sumOf { it.second.toLong() }
        val persona = when {
            totalSteps == 0L && acts.isEmpty() && done.isEmpty() && lifts.isEmpty() -> WrappedPersona.FRESH_START
            lifts.size >= 20 && lifts.size >= acts.size -> WrappedPersona.IRON_LIFTER
            totalKm >= 100 -> WrappedPersona.ROAD_WARRIOR
            done.size >= 200 -> WrappedPersona.HABIT_HERO
            totalSteps >= 1_000_000 -> WrappedPersona.STEP_MACHINE
            else -> WrappedPersona.ALL_ROUNDER
        }

        return WrappedSummary(
            year = year,
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

    /** Wrapped season: December, and January for the year that just ended. */
    fun seasonYear(today: LocalDate): Int? = when (today.monthValue) {
        12 -> today.year
        1 -> today.year - 1
        else -> null
    }
}
