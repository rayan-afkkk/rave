package com.rave.app.domain.habit

/** Gamification points: +10 per habit done, +1 per 100 m tracked, +2 per working set, +50 per competition win. */
object Points {
    const val PER_HABIT = 10
    const val PER_100M = 1
    const val PER_WIN = 50
    const val PER_SET = 2
    const val PER_LEVEL = 500

    fun compute(habitsDone: Int, metersTracked: Double, competitionWins: Int, workingSets: Int = 0): Int =
        habitsDone * PER_HABIT + (metersTracked / 100.0).toInt() * PER_100M + competitionWins * PER_WIN + workingSets * PER_SET

    fun level(totalPoints: Int): Int = 1 + totalPoints.coerceAtLeast(0) / PER_LEVEL

    /** Progress (0..1) towards the next level. */
    fun levelProgress(totalPoints: Int): Float = (totalPoints.coerceAtLeast(0) % PER_LEVEL) / PER_LEVEL.toFloat()
}
