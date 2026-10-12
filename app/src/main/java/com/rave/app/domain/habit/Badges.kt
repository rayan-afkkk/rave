package com.rave.app.domain.habit

enum class BadgeId {
    FIRST_ACTIVITY,
    FIRST_5K,
    RUN_10K,
    KM_100,
    STREAK_7,
    STREAK_30,
    EARLY_BIRD,
    HABITS_100,
    COMPETITION_WINNER,
    SOCIAL,
    FIRST_WORKOUT,
    SETS_100,
}

data class BadgeInputs(
    val totalActivities: Int = 0,
    val longestActivityM: Double = 0.0,
    val longestRunM: Double = 0.0,
    val totalDistanceM: Double = 0.0,
    val bestStreak: Int = 0,
    /** Activities started before 07:00 local time. */
    val earlyActivities: Int = 0,
    val totalHabitCompletions: Int = 0,
    val competitionWins: Int = 0,
    val invitesAccepted: Int = 0,
    val workouts: Int = 0,
    val workingSets: Int = 0,
)

object BadgeEvaluator {
    fun earned(i: BadgeInputs): Set<BadgeId> = buildSet {
        if (i.totalActivities >= 1) add(BadgeId.FIRST_ACTIVITY)
        if (i.longestActivityM >= 5_000) add(BadgeId.FIRST_5K)
        if (i.longestRunM >= 10_000) add(BadgeId.RUN_10K)
        if (i.totalDistanceM >= 100_000) add(BadgeId.KM_100)
        if (i.bestStreak >= 7) add(BadgeId.STREAK_7)
        if (i.bestStreak >= 30) add(BadgeId.STREAK_30)
        if (i.earlyActivities >= 5) add(BadgeId.EARLY_BIRD)
        if (i.totalHabitCompletions >= 100) add(BadgeId.HABITS_100)
        if (i.competitionWins >= 1) add(BadgeId.COMPETITION_WINNER)
        if (i.invitesAccepted >= 3) add(BadgeId.SOCIAL)
        if (i.workouts >= 1) add(BadgeId.FIRST_WORKOUT)
        if (i.workingSets >= 100) add(BadgeId.SETS_100)
    }
}
