package com.rave.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.rave.app.domain.model.ActivityType
import com.rave.app.ui.friends.FriendsScreen
import com.rave.app.ui.home.HomeScreen
import com.rave.app.ui.profile.ProfileScreen
import com.rave.app.ui.routes.RoutesScreen
import com.rave.app.ui.tracking.LiveTrackingScreen

@Composable
fun RaveNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
    onStartActivity: (ActivityType) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        composable<HomeRoute> {
            HomeScreen(contentPadding = contentPadding, onStartActivity = onStartActivity)
        }
        composable<RoutesRoute> { RoutesScreen(contentPadding = contentPadding) }
        composable<FriendsRoute> { FriendsScreen(contentPadding = contentPadding) }
        composable<ProfileRoute> { ProfileScreen(contentPadding = contentPadding) }
        composable<LiveTrackingRoute> { entry ->
            val route = entry.toRoute<LiveTrackingRoute>()
            val type = runCatching { ActivityType.valueOf(route.activityType) }
                .getOrDefault(ActivityType.WALK)
            LiveTrackingScreen(activityType = type, onBack = { navController.popBackStack() })
        }
    }
}
