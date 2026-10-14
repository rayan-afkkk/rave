package com.rave.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.rave.app.R
import kotlin.reflect.KClass

/** The four tabs of the bottom bar (the centre Start button is separate; Routes lives in its sheet). */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val labelRes: Int,
) {
    HOME(HomeRoute, HomeRoute::class, Icons.Rounded.Home, Icons.Outlined.Home, R.string.nav_home),
    SCAN(FoodRoute, FoodRoute::class, Icons.Rounded.QrCodeScanner, Icons.Outlined.QrCodeScanner, R.string.nav_scan),
    FRIENDS(FriendsRoute, FriendsRoute::class, Icons.Rounded.People, Icons.Outlined.People, R.string.nav_friends),
    PROFILE(ProfileRoute, ProfileRoute::class, Icons.Rounded.Person, Icons.Outlined.Person, R.string.nav_profile),
}
