package com.rave.app.domain.food

import kotlin.math.roundToInt

enum class Sex { MALE, FEMALE, UNSPECIFIED }

data class DailyTarget(val kcal: Int, val proteinG: Int, val carbsG: Int, val fatG: Int)

/**
 * Daily calories from the Mifflin-St Jeor equation × an activity factor based on how much the
 * user walks, then adjusted for the goal. Macros: protein by body weight, ~28% fat, rest carbs.
 */
object CalorieTarget {
    fun bmr(weightKg: Float, heightCm: Float, age: Int, sex: Sex): Double {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * age
        return base + when (sex) { Sex.MALE -> 5.0; Sex.FEMALE -> -161.0; Sex.UNSPECIFIED -> -78.0 }
    }

    fun activityFactor(avgDailySteps: Int): Double = when {
        avgDailySteps < 5_000 -> 1.3
        avgDailySteps < 8_000 -> 1.45
        avgDailySteps < 11_000 -> 1.6
        else -> 1.75
    }

    fun target(weightKg: Float, heightCm: Float, age: Int, sex: Sex, avgDailySteps: Int, goal: FoodGoal): DailyTarget {
        val bmr = bmr(weightKg, heightCm, age.coerceIn(14, 90), sex)
        val tdee = bmr * activityFactor(avgDailySteps)
        val kcal = when (goal) {
            FoodGoal.LOSE -> maxOf(tdee - 450, bmr * 1.1, 1_200.0)
            FoodGoal.MAINTAIN -> tdee
            FoodGoal.GAIN -> tdee + 300
        }
        val protein = weightKg * if (goal == FoodGoal.MAINTAIN) 1.2 else 1.6
        val fat = kcal * 0.28 / 9
        val carbs = ((kcal - protein * 4 - fat * 9) / 4).coerceAtLeast(50.0)
        val rounded = (kcal / 10).roundToInt() * 10
        return DailyTarget(rounded, protein.roundToInt(), carbs.roundToInt(), fat.roundToInt())
    }
}
