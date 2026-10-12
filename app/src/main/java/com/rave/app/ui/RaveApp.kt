package com.rave.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rave.app.domain.model.ActivityType
import com.rave.app.ui.components.AppBackground
import com.rave.app.ui.navigation.FloatingBottomBar
import com.rave.app.ui.navigation.FloatingBottomBarClearance
import com.rave.app.ui.navigation.LiveTrackingRoute
import com.rave.app.ui.navigation.RaveNavHost
import com.rave.app.ui.navigation.TopLevelDestination
import com.rave.app.ui.tracking.ActivityPickerSheet

/** Root composable: background, nav host, floating bottom bar and the Start picker. */
@Composable
fun RaveApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val selectedTab = TopLevelDestination.entries.firstOrNull { dest ->
        currentDestination?.hierarchy?.any { it.hasRoute(dest.routeClass) } == true
    }
    // Bar is only shown on the four top-level tabs (hidden on tracking, detail screens…).
    val showBottomBar = selectedTab != null
    var showPicker by rememberSaveable { mutableStateOf(false) }

    val startActivity: (ActivityType) -> Unit = { type ->
        showPicker = false
        navController.navigate(LiveTrackingRoute(type.name)) { launchSingleTop = true }
    }

    val statusBar = WindowInsets.statusBars.asPaddingValues()
    val navBar = WindowInsets.navigationBars.asPaddingValues()
    val contentPadding = PaddingValues(
        top = statusBar.calculateTopPadding(),
        bottom = navBar.calculateBottomPadding() + FloatingBottomBarClearance,
    )

    AppBackground {
        RaveNavHost(
            navController = navController,
            contentPadding = contentPadding,
            onStartActivity = startActivity,
        )

        AnimatedVisibility(
            visible = showBottomBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            FloatingBottomBar(
                selected = selectedTab,
                onSelect = { dest -> navController.navigateToTopLevel(dest) },
                onStartClick = { showPicker = true },
            )
        }

        if (showPicker) {
            ActivityPickerSheet(
                onPick = startActivity,
                onDismiss = { showPicker = false },
            )
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

