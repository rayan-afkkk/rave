package com.rave.app.domain.fitness

/**
 * Everyday-activity estimates from step counts and body stats. These are the standard
 * pedometer approximations (no GPS needed), good to within ~10% for walking.
 */
object FitnessMath {
    const val DEFAULT_STEP_GOAL = 8_000

    /** Walking stride ≈ 41.5% of height. */
    fun strideM(heightCm: Float): Double = (heightCm.coerceIn(100f, 230f) * 0.415) / 100.0

    fun distanceM(steps: Int, heightCm: Float): Double = steps * strideM(heightCm)

    /**
     * Walking burns ≈ 0.57 kcal per mile per lb of body weight, i.e.
     * kcal = miles × 0.57 × lb. Example: 8,000 steps at 170 cm / 70 kg ≈ 310 kcal.
     */
    fun stepCalories(steps: Int, weightKg: Float, heightCm: Float): Int {
        val miles = distanceM(steps, heightCm) / 1609.344
        return (miles * 0.57 * weightKg.coerceIn(30f, 250f) * 2.20462).toInt()
    }

    /**
     * Steps since the previous reading of the phone's cumulative step counter (which counts
     * from the last reboot). First reading only sets the baseline; a smaller value means the
     * phone restarted, so everything since boot is new.
     */
    fun counterDelta(previous: Long?, current: Long): Long = when {
        previous == null -> 0
        current >= previous -> current - previous
        else -> current
    }

    /**
     * Today's steps after a sync: the current total plus what the hardware counter saw since
     * the last reading, or the Recording API's total if that's higher. Never goes down.
     */
    fun mergeSteps(current: Int, recorded: Int?, counterDelta: Long): Int =
        maxOf(current.toLong() + counterDelta.coerceAtLeast(0), (recorded ?: 0).toLong(), current.toLong())
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
