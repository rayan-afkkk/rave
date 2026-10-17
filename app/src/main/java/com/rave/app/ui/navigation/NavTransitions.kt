package com.rave.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute

/*
 * Screen transitions. All of them only move/fade whole layers (no relayout per frame), so they
 * stay smooth even on slower phones.
 *  - Switching tabs slides a short distance in the direction of the tapped tab.
 *  - Opening a screen slides it in from the right; going back reverses that.
 */

private const val TAB_MS = 240
private const val PUSH_MS = 280
private val Ease = FastOutSlowInEasing

private fun NavBackStackEntry.tabIndex(): Int =
    TopLevelDestination.entries.indexOfFirst { destination.hasRoute(it.routeClass) }

private typealias Scope = AnimatedContentTransitionScope<NavBackStackEntry>

/** +1 when moving to a tab on the right, -1 to the left, 0 when either side isn't a tab. */
private fun Scope.tabDirection(): Int {
    val from = initialState.tabIndex()
    val to = targetState.tabIndex()
    return if (from < 0 || to < 0) 0 else Integer.signum(to - from)
}

fun Scope.raveEnter(): EnterTransition {
    val dir = tabDirection()
    return if (dir != 0) {
        slideInHorizontally(tween(TAB_MS, easing = Ease)) { w -> dir * w / 6 } + fadeIn(tween(TAB_MS))
    } else if (targetState.tabIndex() >= 0) {
        fadeIn(tween(TAB_MS))
    } else {
        slideInHorizontally(tween(PUSH_MS, easing = Ease)) { w -> w / 4 } + fadeIn(tween(PUSH_MS)) +
            scaleIn(tween(PUSH_MS, easing = Ease), initialScale = 0.98f)
    }
}

fun Scope.raveExit(): ExitTransition {
    val dir = tabDirection()
    return if (dir != 0) {
        slideOutHorizontally(tween(TAB_MS, easing = Ease)) { w -> -dir * w / 6 } + fadeOut(tween(TAB_MS / 2))
    } else {
        fadeOut(tween(PUSH_MS / 2)) + scaleOut(tween(PUSH_MS, easing = Ease), targetScale = 0.98f)
    }
}

fun Scope.ravePopEnter(): EnterTransition =
    if (tabDirection() != 0) raveEnter()
    else fadeIn(tween(PUSH_MS)) + scaleIn(tween(PUSH_MS, easing = Ease), initialScale = 0.98f)

fun Scope.ravePopExit(): ExitTransition =
    if (tabDirection() != 0) raveExit()
    else slideOutHorizontally(tween(PUSH_MS, easing = Ease)) { w -> w / 4 } + fadeOut(tween(PUSH_MS / 2))
