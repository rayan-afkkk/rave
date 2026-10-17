package com.rave.app.domain.model

/** Appearance setting. */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** Visual style: the original warm orange / off-white look (default), or the bold Bento tiles. */
enum class AppStyle { BENTO, CLASSIC }

enum class ProfileVisibility { FRIENDS, PRIVATE }

/** Notification categories users can toggle individually. */
enum class NotificationCategory { REMINDERS, FRIENDS, COMPETITIONS, AUTO_DETECT, STEP_NUDGES, WEEKLY_REPORT }

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val appStyle: AppStyle = AppStyle.CLASSIC,
    val unit: DistanceUnit = DistanceUnit.KM,
    val weightKg: Float = 70f,
    val heightCm: Float = 170f,
    val dailyGoalKm: Float = 3f,
    val dailyStepGoal: Int = 8_000,
    val waterGoalGlasses: Int = 8,
    /** 0 = not set (assume 25 for calorie targets). */
    val birthYear: Int = 0,
    val sex: com.rave.app.domain.food.Sex = com.rave.app.domain.food.Sex.UNSPECIFIED,
    val food: com.rave.app.domain.food.FoodPreferences = com.rave.app.domain.food.FoodPreferences(),
    val budgetMeals: Boolean = false,
    val autoDetectWalks: Boolean = false,
    val batterySaverGps: Boolean = false,
    val autoPause: Boolean = true,
    val voiceFeedback: Boolean = false,
    val hideStartEnd: Boolean = true,
    val visibility: ProfileVisibility = ProfileVisibility.FRIENDS,
    val notifications: Set<NotificationCategory> = NotificationCategory.entries.toSet(),
    val displayName: String = "",
    val onboardingDone: Boolean = false,
)
