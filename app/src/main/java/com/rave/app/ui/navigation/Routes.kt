package com.rave.app.ui.navigation

import kotlinx.serialization.Serializable

/* Type-safe navigation destinations. */

@Serializable data object HomeRoute
@Serializable data object RoutesRoute
@Serializable data object FriendsRoute
@Serializable data object ProfileRoute

/** Live GPS tracking. [activityType] is an [com.rave.app.domain.model.ActivityType] name. */
@Serializable data class LiveTrackingRoute(val activityType: String)
