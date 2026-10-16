package com.rave.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rave.app.MainViewModel
import com.rave.app.R
import com.rave.app.core.DeepLink
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.util.Format
import com.rave.app.tracking.TrackingState
import com.rave.app.ui.components.AppBackground
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.navigation.ActivitySummaryRoute
import com.rave.app.ui.navigation.CompetitionDetailRoute
import com.rave.app.ui.navigation.BottomBarClearance
import com.rave.app.ui.navigation.RaveBottomBar
import com.rave.app.ui.navigation.FriendRequestsRoute
import com.rave.app.ui.navigation.HomeRoute
import com.rave.app.ui.navigation.InviteLandingRoute
import com.rave.app.ui.navigation.LiveTrackingRoute
import com.rave.app.ui.navigation.OnboardingRoute
import com.rave.app.ui.navigation.RaveNavHost
import com.rave.app.ui.navigation.RoutesRoute
import com.rave.app.ui.navigation.SharedStreaksRoute
import com.rave.app.ui.navigation.TopLevelDestination
import com.rave.app.ui.navigation.WorkoutRoute
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import com.rave.app.ui.theme.PillShape
import com.rave.app.ui.tracking.ActivityPickerSheet

/** Root composable: background, nav host, floating bottom bar, Start picker, deep links. */
@Composable
fun RaveApp(viewModel: MainViewModel, navController: NavHostController = rememberNavController()) {
    val needsOnboarding by viewModel.needsOnboarding.collectAsStateWithLifecycle()
    if (needsOnboarding == null) {
        AppBackground { LoadingState() }
        return
    }
    LaunchedEffect(needsOnboarding) {
        if (needsOnboarding == true && !viewModel.onboardingDone.value) navController.navigate(OnboardingRoute) { launchSingleTop = true }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val selectedTab = TopLevelDestination.entries.firstOrNull { dest ->
        currentDestination?.hierarchy?.any { it.hasRoute(dest.routeClass) } == true
    }
    val onTracking = currentDestination?.hierarchy?.any { it.hasRoute(LiveTrackingRoute::class) } == true
    val onOnboarding = currentDestination?.hierarchy?.any { it.hasRoute(OnboardingRoute::class) } == true
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val tracking by viewModel.trackingState.collectAsStateWithLifecycle()
    val deepLink by viewModel.pendingDeepLink.collectAsStateWithLifecycle()
    val pendingInvite by viewModel.pendingInvite.collectAsStateWithLifecycle()
    val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()

    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val onWorkout = currentDestination?.hierarchy?.any { it.hasRoute(WorkoutRoute::class) } == true
    val startWorkout: () -> Unit = {
        showPicker = false
        navController.navigate(WorkoutRoute) { launchSingleTop = true }
    }
    val startActivity: (ActivityType) -> Unit = { type ->
        showPicker = false
        navController.navigate(LiveTrackingRoute(tracking.type.takeIf { tracking.isTracking }?.name ?: type.name)) { launchSingleTop = true }
    }

    // Route deep links once the user is past onboarding.
    LaunchedEffect(deepLink, onboardingDone, backStackEntry) {
        val link = deepLink ?: return@LaunchedEffect
        if (!onboardingDone || backStackEntry == null) return@LaunchedEffect
        viewModel.consumeDeepLink()
        when (link) {
            is DeepLink.Invite -> navController.navigate(InviteLandingRoute(link.code))
            is DeepLink.CompetitionLink -> navController.navigate(CompetitionDetailRoute(link.id))
            is DeepLink.ActivityLink -> navController.navigate(ActivitySummaryRoute(link.id, link.isNew))
            DeepLink.Tracking -> if (tracking.isTracking) startActivity(tracking.type)
            DeepLink.Profile -> navController.navigateToTopLevel(TopLevelDestination.PROFILE)
            DeepLink.Friends -> navController.navigateToTopLevel(TopLevelDestination.FRIENDS)
            DeepLink.FriendRequests -> navController.navigate(FriendRequestsRoute)
            DeepLink.SharedStreaks -> navController.navigate(SharedStreaksRoute)
            is DeepLink.StartActivity -> startActivity(link.type)
            DeepLink.Home -> navController.navigateToTopLevel(TopLevelDestination.HOME)
        }
    }
    // An invite captured before sign-up (install referrer / link) resumes once onboarding is done.
    LaunchedEffect(Unit) {
        viewModel.paywallRequests.collect { reason ->
            navController.navigate(com.rave.app.ui.navigation.PaywallRoute(reason.name)) { launchSingleTop = true }
        }
    }
    LaunchedEffect(pendingInvite, onboardingDone) {
        val code = pendingInvite ?: return@LaunchedEffect
        if (onboardingDone && deepLink == null && !onOnboarding) navController.navigate(InviteLandingRoute(code)) { launchSingleTop = true }
    }

    val statusBar = WindowInsets.statusBars.asPaddingValues()
    val navBar = WindowInsets.navigationBars.asPaddingValues()
    val contentPadding = PaddingValues(
        top = statusBar.calculateTopPadding(),
        bottom = navBar.calculateBottomPadding() + BottomBarClearance,
    )

    AppBackground {
        RaveNavHost(
            navController = navController,
            startDestination = HomeRoute,
            contentPadding = contentPadding,
            onStartActivity = startActivity,
            onStartWorkout = startWorkout,
        )

        AnimatedVisibility(
            visible = selectedTab != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            RaveBottomBar(
                selected = selectedTab,
                onSelect = { dest -> navController.navigateToTopLevel(dest) },
                onStartClick = { if (tracking.isTracking) startActivity(tracking.type) else showPicker = true },
            )
        }

        // "Walk in progress" banner when tracking runs in the background.
        if (tracking.isTracking && !onTracking) {
            TrackingBanner(tracking, Modifier.align(Alignment.TopCenter)) { startActivity(tracking.type) }
        } else if (activeWorkout != null && !onWorkout && !onOnboarding) {
            WorkoutBanner(Modifier.align(Alignment.TopCenter), onClick = startWorkout)
        }

        if (showPicker) {
            ActivityPickerSheet(
                onPick = startActivity,
                onWorkout = startWorkout,
                onRoutes = { showPicker = false; navController.navigate(RoutesRoute) { launchSingleTop = true } },
                onDismiss = { showPicker = false },
            )
        }
    }
}

@Composable
private fun TrackingBanner(state: TrackingState, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = PillShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.statusBarsPadding().padding(top = 8.dp, start = 16.dp, end = 16.dp).fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(activityTypeIcon(state.type), null)
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.tracking_banner, stringResource(activityTypeRes(state.type)),
                    Format.distance(state.distanceM, DistanceUnit.KM), Format.duration(state.elapsedMs / 1000)),
                style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.tracking_banner_return), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun WorkoutBanner(modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = PillShape,
        color = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        modifier = modifier.statusBarsPadding().padding(top = 8.dp, start = 16.dp, end = 16.dp).fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.FitnessCenter, null)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.workout_banner), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.tracking_banner_return), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Standard bottom-nav behaviour: single top, keep one copy of each tab, restore tab state. */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
