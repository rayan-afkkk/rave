package com.rave.app.domain.buddy

/** Growth stages of the Rave buddy. */
enum class BuddyStage(val fromLevel: Int) { SPROUT(1), BUDDY(3), RUNNER(6), CHAMP(10), LEGEND(15) }

/** How the buddy feels today. It never dies or loses levels: on lazy days it just gets sleepy. */
enum class BuddyMood { PUMPED, HAPPY, OKAY, SLEEPY }

data class BuddyState(
    val xp: Long,
    val level: Int,
    val stage: BuddyStage,
    /** XP earned inside the current level and needed to reach the next one. */
    val xpIntoLevel: Long,
    val xpForNextLevel: Long,
    val mood: BuddyMood,
) {
    val levelProgress: Float get() = if (xpForNextLevel <= 0) 1f else (xpIntoLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)
}

/**
 * A companion that grows with everything you do: steps, habits, walks/runs and workouts.
 * XP only goes up (no punishment for missed days); today's activity sets the mood.
 */
object BuddyEngine {
    const val XP_PER_100_STEPS = 1
    const val XP_PER_HABIT = 10
    const val XP_PER_KM = 20
    const val XP_PER_WORKOUT = 25

    fun xp(totalSteps: Long, habitCompletions: Int, totalKm: Double, workouts: Int): Long =
        totalSteps / 100 * XP_PER_100_STEPS + habitCompletions * XP_PER_HABIT +
            (totalKm * XP_PER_KM).toLong() + workouts * XP_PER_WORKOUT

    /** XP needed to go from level n to n+1: 100, 150, 200, … (gentle curve). */
    fun xpToNext(level: Int): Long = 100L + (level - 1) * 50L

    fun state(xp: Long, stepsToday: Int, stepGoal: Int, didSomethingToday: Boolean, hour: Int): BuddyState {
        var level = 1
        var remaining = xp
        while (remaining >= xpToNext(level)) {
            remaining -= xpToNext(level)
            level++
        }
        val stage = BuddyStage.entries.last { level >= it.fromLevel }
        val mood = when {
            stepGoal > 0 && stepsToday >= stepGoal -> BuddyMood.PUMPED
            didSomethingToday || stepsToday >= stepGoal / 2 -> BuddyMood.HAPPY
            hour >= 18 && stepsToday < stepGoal / 4 -> BuddyMood.SLEEPY
            else -> BuddyMood.OKAY
        }
        return BuddyState(xp, level, stage, remaining, xpToNext(level), mood)
    }
}
