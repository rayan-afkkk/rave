package com.rave.app.domain.model

/** Appearance setting. */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class ProfileVisibility { FRIENDS, PRIVATE }

/** Notification categories users can toggle individually. */
enum class NotificationCategory { REMINDERS, FRIENDS, COMPETITIONS, AUTO_DETECT }

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val unit: DistanceUnit = DistanceUnit.KM,
    val weightKg: Float = 70f,
    val heightCm: Float = 170f,
    val dailyGoalKm: Float = 3f,
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
