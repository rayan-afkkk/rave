package com.rave.app.domain.workout

/** Muscle group used to group and filter exercises. */
enum class MuscleGroup { CHEST, BACK, LEGS, SHOULDERS, ARMS, CORE, FULL_BODY }

/** How a set is measured. */
enum class ExerciseKind {
    /** Weight × reps (bench press, squat…). */
    WEIGHT_REPS,
    /** Reps only (push-ups, pull-ups…). */
    BODYWEIGHT_REPS,
    /** Held for time (plank…). */
    DURATION,
}

data class Exercise(
    val id: Long = 0,
    val name: String,
    val muscle: MuscleGroup,
    val kind: ExerciseKind,
    val custom: Boolean = false,
)

/** One logged set. Only [done] non-warm-up sets count towards volume and PRs. */
data class SetEntry(
    val id: Long = 0,
    val exerciseId: Long,
    val order: Int = 0,
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val durationS: Int = 0,
    val warmup: Boolean = false,
    val done: Boolean = false,
) {
    val counts: Boolean get() = done && !warmup
}

enum class SetPr { HEAVIEST, BEST_1RM, MOST_REPS, BEST_VOLUME, LONGEST_HOLD }

/** Best marks for one exercise. */
data class ExerciseBests(
    val heaviestKg: Double = 0.0,
    val best1rmKg: Double = 0.0,
    val mostReps: Int = 0,
    val bestSetVolumeKg: Double = 0.0,
    val longestHoldS: Int = 0,
) {
    val isEmpty: Boolean get() = heaviestKg == 0.0 && mostReps == 0 && longestHoldS == 0
}
