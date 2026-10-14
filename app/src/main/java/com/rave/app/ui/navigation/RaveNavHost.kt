package com.rave.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rave.app.domain.model.ActivityType
import com.rave.app.ui.activity.ActivitySummaryScreen
import com.rave.app.ui.auth.SignInScreen
import com.rave.app.ui.competitions.CompetitionDetailScreen
import com.rave.app.ui.competitions.CompetitionResultScreen
import com.rave.app.ui.competitions.CompetitionsScreen
import com.rave.app.ui.competitions.CreateCompetitionScreen
import com.rave.app.ui.friends.AddFriendScreen
import com.rave.app.ui.friends.FriendRequestsScreen
import com.rave.app.ui.friends.FriendsScreen
import com.rave.app.ui.friends.SharedStreaksScreen
import com.rave.app.ui.habits.AddEditHabitScreen
import com.rave.app.ui.habits.HabitTimerScreen
import com.rave.app.ui.home.HomeScreen
import com.rave.app.ui.invite.InviteLandingScreen
import com.rave.app.ui.invite.InviteScreen
import com.rave.app.ui.invite.QrScanScreen
import com.rave.app.ui.onboarding.OnboardingScreen
import com.rave.app.ui.profile.ProfileScreen
import com.rave.app.ui.progress.ProgressScreen
import com.rave.app.ui.routes.RoutesScreen
import com.rave.app.ui.settings.PrivacyZonesScreen
import com.rave.app.ui.settings.SettingsScreen
import com.rave.app.ui.share.ShareActivityScreen
import com.rave.app.ui.tracking.LiveTrackingScreen
import com.rave.app.ui.workout.WorkoutDetailScreen
import com.rave.app.ui.workout.WorkoutHistoryScreen
import com.rave.app.ui.workout.WorkoutScreen

@Composable
fun RaveNavHost(
    navController: NavHostController,
    startDestination: Any,
    contentPadding: PaddingValues,
    onStartActivity: (ActivityType) -> Unit,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val back: () -> Unit = { navController.popBackStack() }
    fun goHome() = navController.navigate(HomeRoute) { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        // ---- Tabs ----
        composable<HomeRoute> {
            HomeScreen(
                contentPadding = contentPadding,
                onStartActivity = onStartActivity,
                onAddHabit = { navController.navigate(AddEditHabitRoute()) },
                onEditHabit = { navController.navigate(AddEditHabitRoute(it)) },
                onOpenTimer = { navController.navigate(HabitTimerRoute(it)) },
                onOpenProgress = { navController.navigate(ProgressRoute) },
                onOpenBatteryGuide = { navController.navigate(BatteryGuideRoute) },
                onOpenWrapped = { navController.navigate(WrappedRoute(it)) },
            )
        }
        composable<RoutesRoute> {
            RoutesScreen(
                onBack = back,
                contentPadding = contentPadding,
                onOpenActivity = { navController.navigate(ActivitySummaryRoute(it)) },
                onOpenWorkouts = { navController.navigate(WorkoutHistoryRoute) },
            )
        }
        composable<FriendsRoute> {
            FriendsScreen(
                contentPadding = contentPadding,
                onSignIn = { navController.navigate(SignInRoute) },
                onRequests = { navController.navigate(FriendRequestsRoute) },
                onAddFriend = { navController.navigate(AddFriendRoute) },
                onInvite = { navController.navigate(InviteRoute) },
                onCompetitions = { navController.navigate(CompetitionsRoute) },
                onSharedStreaks = { navController.navigate(SharedStreaksRoute) },
            )
        }
        composable<ProfileRoute> {
            ProfileScreen(
                contentPadding = contentPadding,
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenProgress = { navController.navigate(ProgressRoute) },
                onOpenWrapped = { navController.navigate(WrappedRoute(it)) },
                onSignIn = { navController.navigate(SignInRoute) },
            )
        }

        // ---- Onboarding & auth ----
        composable<OnboardingRoute> {
            OnboardingScreen(onFinished = { openEmailSignIn ->
                goHome()
                if (openEmailSignIn) navController.navigate(SignInRoute)
            })
        }
        composable<SignInRoute> { SignInScreen(onDone = back, onSkip = back) }

        // ---- Habits ----
        composable<AddEditHabitRoute> { AddEditHabitScreen(onDone = back, onSignIn = { navController.navigate(SignInRoute) }) }
        composable<HabitTimerRoute> { HabitTimerScreen(onBack = back) }
        composable<ProgressRoute> { ProgressScreen(onBack = back) }

        // ---- Tracking ----
        composable<LiveTrackingRoute> {
            LiveTrackingScreen(
                onBack = back,
                onFinished = { id ->
                    navController.navigate(ActivitySummaryRoute(id, isNew = true)) {
                        popUpTo<LiveTrackingRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<ActivitySummaryRoute> {
            ActivitySummaryScreen(
                onClose = back,
                onShare = { navController.navigate(ShareActivityRoute(it)) },
                onRepeat = { type, id -> navController.navigate(LiveTrackingRoute(type.name, id)) },
            )
        }
        composable<ShareActivityRoute> { ShareActivityScreen(onBack = back) }

        // ---- Strength workouts ----
        composable<WorkoutRoute> {
            WorkoutScreen(onBack = back, onFinished = { id ->
                navController.navigate(WorkoutDetailRoute(id, isNew = true)) { popUpTo<WorkoutRoute> { inclusive = true } }
            })
        }
        composable<WorkoutDetailRoute> { WorkoutDetailScreen(onClose = back) }
        composable<WorkoutHistoryRoute> {
            WorkoutHistoryScreen(onBack = back, onStart = onStartWorkout, onOpen = { navController.navigate(WorkoutDetailRoute(it)) })
        }

        // ---- Settings ----
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = back,
                onPrivacyZones = { navController.navigate(PrivacyZonesRoute) },
                onBatteryGuide = { navController.navigate(BatteryGuideRoute) },
                onSignIn = { navController.navigate(SignInRoute) },
                onAccountGone = { navController.navigate(OnboardingRoute) { popUpTo(navController.graph.id) { inclusive = true } } },
            )
        }
        composable<PrivacyZonesRoute> { PrivacyZonesScreen(onBack = back) }
        composable<BatteryGuideRoute> { com.rave.app.ui.settings.BatteryGuideScreen(onBack = back) }
        composable<WrappedRoute> { com.rave.app.ui.wrapped.WrappedScreen(onBack = back) }

        // ---- Food ----
        composable<FoodRoute> {
            com.rave.app.ui.food.FoodScreen(
                contentPadding = contentPadding,
                onScanBarcode = { navController.navigate(FoodScanRoute) },
                onOpenResult = { navController.navigate(FoodResultRoute(it)) },
                onOpenPlan = { navController.navigate(MealPlanRoute) },
                onOpenSettings = { navController.navigate(FoodSettingsRoute) },
            )
        }
        composable<FoodScanRoute> {
            com.rave.app.ui.food.FoodScanScreen(
                onBack = back,
                onBarcode = { code ->
                    navController.navigate(FoodResultRoute(code)) { popUpTo<FoodScanRoute> { inclusive = true } }
                },
            )
        }
        composable<FoodResultRoute> {
            com.rave.app.ui.food.FoodResultScreen(
                onBack = back,
                onScanAnother = {
                    navController.navigate(FoodScanRoute) { popUpTo<FoodResultRoute> { inclusive = true } }
                },
            )
        }
        composable<MealPlanRoute> {
            com.rave.app.ui.food.MealPlanScreen(onBack = back, onOpenSettings = { navController.navigate(FoodSettingsRoute) })
        }
        composable<FoodSettingsRoute> { com.rave.app.ui.food.FoodSettingsScreen(onBack = back) }

        // ---- Social ----
        composable<FriendRequestsRoute> {
            FriendRequestsScreen(onBack = back, onOpenCompetition = { navController.navigate(CompetitionDetailRoute(it)) })
        }
        composable<AddFriendRoute> {
            AddFriendScreen(
                onBack = back,
                onScanQr = { navController.navigate(QrScanRoute) },
                onRedeemCode = { navController.navigate(InviteLandingRoute(it)) },
            )
        }
        composable<InviteRoute> { InviteScreen(onBack = back) }
        composable<QrScanRoute> {
            QrScanScreen(onBack = back, onCode = { code ->
                navController.navigate(InviteLandingRoute(code)) { popUpTo<QrScanRoute> { inclusive = true } }
            })
        }
        composable<InviteLandingRoute> {
            InviteLandingScreen(
                onClose = back,
                onSignIn = { navController.navigate(SignInRoute) },
                onOpenCompetition = { id ->
                    navController.navigate(CompetitionDetailRoute(id)) { popUpTo<InviteLandingRoute> { inclusive = true } }
                },
            )
        }
        composable<SharedStreaksRoute> { SharedStreaksScreen(onBack = back) }

        // ---- Competitions ----
        composable<CompetitionsRoute> {
            CompetitionsScreen(
                onBack = back,
                onCreate = { navController.navigate(CreateCompetitionRoute) },
                onOpen = { navController.navigate(CompetitionDetailRoute(it)) },
                onResults = { navController.navigate(CompetitionResultRoute(it)) },
            )
        }
        composable<CreateCompetitionRoute> {
            CreateCompetitionScreen(onBack = back, onCreated = { id ->
                navController.navigate(CompetitionDetailRoute(id)) { popUpTo<CreateCompetitionRoute> { inclusive = true } }
            })
        }
        composable<CompetitionDetailRoute> {
            CompetitionDetailScreen(onBack = back, onResults = { navController.navigate(CompetitionResultRoute(it)) })
        }
        composable<CompetitionResultRoute> { CompetitionResultScreen(onBack = back) }
    }
}
