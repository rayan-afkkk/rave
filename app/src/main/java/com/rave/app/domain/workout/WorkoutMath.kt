package com.rave.app.domain.workout

/** Strength-training maths: estimated 1RM, volume, bests and PR detection. */
object WorkoutMath {

    /** Epley estimated one-rep max. 1 rep = the weight itself; 0 reps = 0. */
    fun estimated1rm(weightKg: Double, reps: Int): Double = when {
        reps <= 0 || weightKg <= 0 -> 0.0
        reps == 1 -> weightKg
        else -> weightKg * (1 + reps / 30.0)
    }

    /** Total weight lifted (weight × reps) over completed working sets. */
    fun volume(sets: List<SetEntry>): Double = sets.filter { it.counts }.sumOf { it.weightKg * it.reps }

    fun bests(sets: List<SetEntry>): ExerciseBests {
        val working = sets.filter { it.counts }
        return ExerciseBests(
            heaviestKg = working.filter { it.reps > 0 }.maxOfOrNull { it.weightKg } ?: 0.0,
            best1rmKg = working.maxOfOrNull { estimated1rm(it.weightKg, it.reps) } ?: 0.0,
            mostReps = working.maxOfOrNull { it.reps } ?: 0,
            bestSetVolumeKg = working.maxOfOrNull { it.weightKg * it.reps } ?: 0.0,
            longestHoldS = working.maxOfOrNull { it.durationS } ?: 0,
        )
    }

    /**
     * PRs set by [sets] compared with all-time [previous] bests for the same exercise.
     * Like distance PBs, the very first session never counts as a PR.
     */
    fun detectPrs(previous: ExerciseBests, sets: List<SetEntry>, kind: ExerciseKind): Set<SetPr> {
        if (previous.isEmpty) return emptySet()
        val now = bests(sets)
        return buildSet {
            when (kind) {
                ExerciseKind.WEIGHT_REPS -> {
                    if (now.heaviestKg > previous.heaviestKg) add(SetPr.HEAVIEST)
                    if (now.best1rmKg > previous.best1rmKg + 0.01) add(SetPr.BEST_1RM)
                    if (now.bestSetVolumeKg > previous.bestSetVolumeKg) add(SetPr.BEST_VOLUME)
                }
                ExerciseKind.BODYWEIGHT_REPS -> if (now.mostReps > previous.mostReps) add(SetPr.MOST_REPS)
                ExerciseKind.DURATION -> if (now.longestHoldS > previous.longestHoldS) add(SetPr.LONGEST_HOLD)
            }
        }
    }

    /** Merges bests from several past sessions. */
    fun merge(a: ExerciseBests, b: ExerciseBests) = ExerciseBests(
        heaviestKg = maxOf(a.heaviestKg, b.heaviestKg),
        best1rmKg = maxOf(a.best1rmKg, b.best1rmKg),
        mostReps = maxOf(a.mostReps, b.mostReps),
        bestSetVolumeKg = maxOf(a.bestSetVolumeKg, b.bestSetVolumeKg),
        longestHoldS = maxOf(a.longestHoldS, b.longestHoldS),
    )

    /** kg ↔ lb for display when the user prefers imperial units. */
    const val LB_PER_KG = 2.2046226218
}
