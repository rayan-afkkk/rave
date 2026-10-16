package com.rave.app.ui.navigation

import kotlinx.serialization.Serializable

/* Type-safe navigation destinations. */

// Bottom-bar tabs
@Serializable data object HomeRoute
@Serializable data object RoutesRoute
@Serializable data object FriendsRoute
@Serializable data object ProfileRoute

// Onboarding & auth
@Serializable data object OnboardingRoute
@Serializable data object SignInRoute

// Habits
@Serializable data class AddEditHabitRoute(val habitId: Long = -1)
@Serializable data class HabitTimerRoute(val habitId: Long)
@Serializable data object ProgressRoute

// Tracking & activities
/** [activityType] is an [com.rave.app.domain.model.ActivityType] name; [guideActivityId] a favourite route to follow. */
@Serializable data class LiveTrackingRoute(val activityType: String, val guideActivityId: Long = -1)
@Serializable data class ActivitySummaryRoute(val activityId: Long, val isNew: Boolean = false)
@Serializable data class ShareActivityRoute(val activityId: Long)

// Settings
@Serializable data object SettingsRoute
@Serializable data object PrivacyZonesRoute
@Serializable data object BatteryGuideRoute
@Serializable data class WrappedRoute(val year: Int)

// Social
@Serializable data object FriendRequestsRoute
@Serializable data object AddFriendRoute
@Serializable data object InviteRoute
@Serializable data object QrScanRoute
@Serializable data class InviteLandingRoute(val code: String)
@Serializable data object SharedStreaksRoute

// Strength workouts (set tracker)
@Serializable data object WorkoutRoute
@Serializable data class WorkoutDetailRoute(val workoutId: Long, val isNew: Boolean = false)
@Serializable data object WorkoutHistoryRoute

// Competitions
@Serializable data object CompetitionsRoute
@Serializable data object CreateCompetitionRoute
@Serializable data class CompetitionDetailRoute(val id: String)
@Serializable data class CompetitionResultRoute(val id: String)

// Food: scanner, log, meal planner
@Serializable data object FoodRoute
@Serializable data object FoodScanRoute
@Serializable data class FoodResultRoute(val barcode: String = "")
@Serializable data object MealPlanRoute
@Serializable data object FoodSettingsRoute

// Rave Pro
@Serializable data class PaywallRoute(val reason: String = "GENERAL")
@Serializable data object OfflineMapsRoute
