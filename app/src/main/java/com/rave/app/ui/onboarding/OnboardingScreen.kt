package com.rave.app.ui.onboarding

import android.Manifest
import com.rave.app.ui.theme.Dimens
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.ui.auth.SignInViewModel
import com.rave.app.ui.components.AppBackground
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.permissions.hasPermission
import com.rave.app.ui.permissions.notificationPermissions
import com.rave.app.ui.theme.PillShape
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.theme.TileColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    fun finish(name: String, then: () -> Unit) {
        viewModelScope.launch {
            settings.update { it.copy(onboardingDone = true, displayName = name.trim().ifEmpty { it.displayName }) }
            then()
        }
    }
}

private enum class Page { WELCOME, HABITS, GPS, FRIENDS, WORKOUT, SETUP, SIGN_IN }

private data class StoryPage(val title: Int, val body: Int, val tile: @Composable () -> TileColor)

/**
 * First-run experience: five animated story pages (each illustration replays when you reach
 * it, with parallax between pages and a stretching page indicator), a setup page that explains
 * permissions before asking, and finally Google sign-in (guest mode stays available).
 */
@Composable
fun OnboardingScreen(
    onFinished: (openEmailSignIn: Boolean) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
    signIn: SignInViewModel = hiltViewModel(),
) {
    val pages = Page.entries
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    val signInState by signIn.state.collectAsStateWithLifecycle()
    val current = pages[pager.currentPage]

    LaunchedEffect(signInState.done) { if (signInState.done) viewModel.finish(name) { onFinished(false) } }
    BackHandler(enabled = pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }

    val story = mapOf(
        Page.WELCOME to StoryPage(R.string.onb_welcome_title, R.string.onb_welcome_body) { RaveTheme.colors.tilePeach },
        Page.HABITS to StoryPage(R.string.onboarding_habits_title, R.string.onboarding_habits_body) { RaveTheme.colors.tilePink },
        Page.GPS to StoryPage(R.string.onboarding_gps_title, R.string.onboarding_gps_body) { RaveTheme.colors.tileGreen },
        Page.FRIENDS to StoryPage(R.string.onboarding_friends_title, R.string.onboarding_friends_body) { RaveTheme.colors.tileBlue },
        Page.WORKOUT to StoryPage(R.string.onb_workout_title, R.string.onb_workout_body) { RaveTheme.colors.tilePeach },
    )
    val blobTint by animateColorAsState(
        (story[current]?.tile?.invoke() ?: RaveTheme.colors.tilePeach).container,
        animationSpec = tween(600), label = "blobTint",
    )

    AppBackground {
        DriftingBlobs(blobTint)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            // Skip jumps straight to setup.
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                if (pager.currentPage < Page.SETUP.ordinal) {
                    TextButton(onClick = { scope.launch { pager.animateScrollToPage(Page.SETUP.ordinal) } }) { Text(stringResource(R.string.action_skip)) }
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f), userScrollEnabled = current != Page.SIGN_IN || !signInState.busy) { index ->
                val page = pages[index]
                // How far this page is from the centre (−1..1) drives the parallax.
                val offset = (pager.currentPage - index) + pager.currentPageOffsetFraction
                val active = pager.currentPage == index
                when (page) {
                    Page.SETUP -> SetupPage(name) { name = it }
                    Page.SIGN_IN -> SignInPage(
                        busy = signInState.busy,
                        error = signInState.error,
                        available = signIn.available,
                        onGoogle = { signIn.google(it) },
                        onEmail = { viewModel.finish(name) { onFinished(true) } },
                        onGuest = { viewModel.finish(name) { onFinished(false) } },
                    )
                    else -> {
                        val s = story.getValue(page)
                        StoryLayout(offset, s.title, s.body) {
                            when (page) {
                                Page.WELCOME -> WelcomeArt(active, Modifier.fillMaxSize())
                                Page.HABITS -> HabitsArt(active, Modifier.fillMaxSize())
                                Page.GPS -> GpsArt(active, Modifier.fillMaxSize())
                                Page.FRIENDS -> FriendsArt(active, Modifier.fillMaxSize())
                                else -> WorkoutArt(active, Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }

            // Stretching pill indicator.
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
                pages.indices.forEach { i ->
                    val selected = i == pager.currentPage
                    val width by animateDpAsState(if (selected) 26.dp else 8.dp, tween(300), label = "dot")
                    Box(Modifier.padding(horizontal = 3.dp).height(8.dp).width(width).clip(PillShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
                }
            }

            if (current != Page.SIGN_IN) {
                Surface(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = 12.dp).heightIn(min = 56.dp),
                ) {
                    AnimatedContent(
                        targetState = when (current) {
                            Page.WELCOME -> R.string.onb_cta_start
                            Page.SETUP -> R.string.action_continue
                            else -> R.string.action_next
                        },
                        transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
                        label = "cta",
                    ) { label ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/** Illustration on top, text below; text and art move at different speeds while swiping. */
@Composable
private fun StoryLayout(offset: Float, title: Int, body: Int, art: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.weight(1f).fillMaxWidth().graphicsLayer {
                translationX = offset * size.width * 0.35f
                val s = 1f - abs(offset).coerceAtMost(1f) * 0.15f
                scaleX = s; scaleY = s
                alpha = 1f - abs(offset).coerceAtMost(1f) * 0.6f
            },
            contentAlignment = Alignment.Center,
        ) { art() }
        Column(
            Modifier.fillMaxWidth().padding(bottom = 8.dp).graphicsLayer {
                translationX = offset * size.width * 0.7f
                alpha = 1f - abs(offset).coerceAtMost(1f)
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(10.dp))
            Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SetupPage(name: String, onName: (String) -> Unit) {
    val context = LocalContext.current
    var notifGranted by remember { mutableStateOf(notificationPermissions.all { context.hasPermission(it) }) }
    var locationGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        notifGranted = notificationPermissions.all { context.hasPermission(it) }
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        locationGranted = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    LifecycleResumeEffect(Unit) {
        notifGranted = notificationPermissions.all { context.hasPermission(it) }
        locationGranted = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        onPauseOrDispose { }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
    ) {
        Text(stringResource(R.string.onboarding_setup_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        OutlinedTextField(
            value = name, onValueChange = { onName(it.take(40)) },
            label = { Text(stringResource(R.string.onboarding_name)) }, singleLine = true,
            shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.onboarding_permissions_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (notificationPermissions.isNotEmpty()) {
            PermissionCard(Icons.Rounded.Notifications, RaveTheme.colors.tilePeach, R.string.onboarding_perm_notifications,
                R.string.onboarding_perm_notifications_body, notifGranted) { notifLauncher.launch(notificationPermissions.toTypedArray()) }
        }
        PermissionCard(Icons.Rounded.LocationOn, RaveTheme.colors.tileGreen, R.string.onboarding_perm_location,
            R.string.onboarding_perm_location_body, locationGranted) {
            locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        Text(stringResource(R.string.onboarding_perm_activity_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Last step: Google sign-in, with email and guest as secondary options. */
@Composable
private fun SignInPage(
    busy: Boolean,
    error: String?,
    available: Boolean,
    onGoogle: (android.content.Context) -> Unit,
    onEmail: () -> Unit,
    onGuest: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(132.dp).clip(CircleShape).background(RaveTheme.colors.tilePink.container), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Groups, null, tint = RaveTheme.colors.tilePink.content, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.onb_signin_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.onb_signin_body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        listOf(R.string.onb_signin_perk_friends, R.string.onb_signin_perk_compete, R.string.onb_signin_perk_backup).forEach {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        Spacer(Modifier.height(24.dp))
        if (available) {
            Surface(
                onClick = { onGoogle(context) },
                enabled = !busy,
                shape = PillShape,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Rounded.AccountCircle, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.signin_google), style = MaterialTheme.typography.labelLarge)
                }
            }
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            TextButton(onClick = onEmail, enabled = !busy) { Text(stringResource(R.string.onb_signin_email)) }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CloudDone, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.cloud_unavailable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TextButton(onClick = onGuest, enabled = !busy) { Text(stringResource(R.string.signin_continue_guest)) }
    }
}

@Composable
private fun PermissionCard(icon: ImageVector, tile: TileColor, title: Int, body: Int, granted: Boolean, onAllow: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(tile.container), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tile.content)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (granted) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = stringResource(R.string.cd_granted), tint = MaterialTheme.colorScheme.tertiary)
            } else {
                TextButton(onClick = onAllow) { Text(stringResource(R.string.action_allow)) }
            }
        }
    }
}
