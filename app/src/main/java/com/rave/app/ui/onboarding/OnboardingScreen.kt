package com.rave.app.ui.onboarding

import android.Manifest
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rave.app.ui.components.BentoBig
import com.rave.app.ui.components.BentoButton
import com.rave.app.ui.components.BentoChip
import com.rave.app.ui.components.BentoGap
import com.rave.app.ui.components.BentoKicker
import com.rave.app.ui.components.BentoSquareShape
import com.rave.app.ui.components.BentoTile
import com.rave.app.ui.components.BentoTileShape
import com.rave.app.ui.components.BentoTitle
import com.rave.app.ui.components.RaveLogo
import com.rave.app.ui.components.GoogleSignInButton
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

/** Pages that tell the story (the rest are setup + sign-in). */
private val STORY_PAGES = Page.WELCOME.ordinal..Page.WORKOUT.ordinal

/**
 * First-run experience: five story pages (a big colour tile with an animated illustration,
 * then a bold headline), a setup page that explains permissions before asking, and finally
 * Google sign-in (guest mode stays available).
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
    val c = RaveTheme.colors

    LaunchedEffect(signInState.done) { if (signInState.done) viewModel.finish(name) { onFinished(false) } }
    BackHandler(enabled = pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }

    val story = mapOf(
        Page.WELCOME to StoryPage(R.string.onb_welcome_title, R.string.onb_welcome_body) { c.tilePeach },
        Page.HABITS to StoryPage(R.string.onboarding_habits_title, R.string.onboarding_habits_body) { c.tilePink },
        Page.GPS to StoryPage(R.string.onboarding_gps_title, R.string.onboarding_gps_body) { c.tileGreen },
        Page.FRIENDS to StoryPage(R.string.onboarding_friends_title, R.string.onboarding_friends_body) { c.tileBlue },
        Page.WORKOUT to StoryPage(R.string.onb_workout_title, R.string.onb_workout_body) { c.tileYellow },
    )

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding().imePadding(),
    ) {
        // Logo on the left, Skip on the right.
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RaveLogo(color = MaterialTheme.colorScheme.primary, progress = 1f, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.weight(1f))
            if (pager.currentPage < Page.SETUP.ordinal) {
                BentoChip(stringResource(R.string.action_skip), c.tileSand.container, c.tileSand.content,
                    onClick = { scope.launch { pager.animateScrollToPage(Page.SETUP.ordinal) } })
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f), userScrollEnabled = current != Page.SIGN_IN || !signInState.busy) { index ->
            val page = pages[index]
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
                    StoryLayout(offset, index, active, s.tile(), s.title, s.body) {
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

        if (current != Page.SIGN_IN) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Progress: one segment per page, the current one stretched.
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    pages.indices.forEach { i ->
                        val selected = i == pager.currentPage
                        val width by animateDpAsState(if (selected) 24.dp else 7.dp, tween(260), label = "dot")
                        Box(Modifier.height(7.dp).width(width).clip(PillShape)
                            .background(if (i <= pager.currentPage) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline))
                    }
                }
                BentoButton(
                    text = stringResource(
                        when (current) {
                            Page.WELCOME -> R.string.onb_cta_start
                            Page.SETUP -> R.string.action_continue
                            else -> R.string.action_next
                        },
                    ),
                    container = c.cta.container, content = c.cta.content,
                    trailing = Icons.AutoMirrored.Rounded.ArrowForward,
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    height = 54.dp,
                )
            }
        } else {
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Big colour tile with the illustration, then a step label, bold headline and body. */
@Composable
private fun StoryLayout(offset: Float, index: Int, active: Boolean, tile: TileColor, title: Int, body: Int, art: @Composable () -> Unit) {
    // Text rises in (kicker, headline, body one after another) each time the page is reached.
    val rise = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) { rise.snapTo(0f); rise.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
    }
    fun Modifier.rise(order: Int) = graphicsLayer {
        val t = ((rise.value - order * 0.15f) / 0.7f).coerceIn(0f, 1f)
        alpha = t
        translationY = (1f - t) * 28.dp.toPx()
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .graphicsLayer {
                    val s = 1f - abs(offset).coerceAtMost(1f) * 0.08f
                    scaleX = s; scaleY = s
                }
                .clip(BentoTileShape).background(tile.container),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxSize().padding(18.dp).graphicsLayer { translationX = offset * size.width * 0.25f }, contentAlignment = Alignment.Center) { art() }
        }
        Column(
            Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 4.dp).graphicsLayer {
                translationX = offset * size.width * 0.5f
                alpha = 1f - abs(offset).coerceAtMost(1f)
            },
        ) {
            BentoKicker("${index + 1} / ${STORY_PAGES.last + 1}", MaterialTheme.colorScheme.onBackground, Modifier.rise(0))
            Spacer(Modifier.height(6.dp))
            BentoBig(stringResource(title), MaterialTheme.colorScheme.onBackground, size = 30.sp, maxLines = 2,
                modifier = Modifier.rise(1).semantics { heading() })
            Spacer(Modifier.height(8.dp))
            Text(stringResource(body), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 21.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.rise(2))
        }
    }
}

@Composable
private fun SetupPage(name: String, onName: (String) -> Unit) {
    val context = LocalContext.current
    val c = RaveTheme.colors
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
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(BentoGap),
    ) {
        BentoTitle(stringResource(R.string.onboarding_setup_title))
        Spacer(Modifier.height(4.dp))
        // Name, typed straight into a big tile.
        BentoTile(c.tilePeach.container, Modifier.fillMaxWidth(), padding = 18.dp) {
            BentoKicker(stringResource(R.string.onboarding_name), c.tilePeach.content)
            Spacer(Modifier.height(6.dp))
            val style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = c.tilePeach.content)
            BasicTextField(
                value = name, onValueChange = { onName(it.take(40)) }, singleLine = true, textStyle = style,
                cursorBrush = SolidColor(c.tilePeach.content), modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (name.isEmpty()) Text(stringResource(R.string.guest_name), style = style, color = c.tilePeach.content.copy(alpha = 0.35f))
                    inner()
                },
            )
        }
        Text(stringResource(R.string.onboarding_permissions_intro), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        if (notificationPermissions.isNotEmpty()) {
            PermissionTile(Icons.Rounded.Notifications, c.tileBlue, R.string.onboarding_perm_notifications,
                R.string.onboarding_perm_notifications_body, notifGranted) { notifLauncher.launch(notificationPermissions.toTypedArray()) }
        }
        PermissionTile(Icons.Rounded.LocationOn, c.tileGreen, R.string.onboarding_perm_location,
            R.string.onboarding_perm_location_body, locationGranted) {
            locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        Text(stringResource(R.string.onboarding_perm_activity_note), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
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
    val c = RaveTheme.colors
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(BentoGap),
    ) {
        // Dark hero tile with the three things an account unlocks.
        BentoTile(c.tileInk.container, Modifier.fillMaxWidth(), padding = 20.dp) {
            Box(Modifier.size(56.dp).clip(BentoSquareShape).background(c.highlight), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Groups, null, tint = c.onHighlight, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(16.dp))
            BentoBig(stringResource(R.string.onb_signin_title), c.tileInk.content, size = 30.sp, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.onb_signin_body), style = MaterialTheme.typography.bodyLarge, color = c.tileInk.content.copy(alpha = 0.75f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(BentoGap), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            listOf(
                Triple(Icons.Rounded.Groups, c.tilePink, R.string.onb_signin_perk_friends),
                Triple(Icons.Rounded.EmojiEvents, c.tileYellow, R.string.onb_signin_perk_compete),
                Triple(Icons.Rounded.CloudDone, c.tileBlue, R.string.onb_signin_perk_backup),
            ).forEach { (icon, tile, text) ->
                BentoTile(tile.container, Modifier.weight(1f).fillMaxHeight(), padding = 12.dp) {
                    Icon(icon, null, tint = tile.content, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(text), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = tile.content)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        if (available) {
            GoogleSignInButton(onClick = { onGoogle(context) }, busy = busy)
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            }
            BentoButton(stringResource(R.string.onb_signin_email), c.tileSand.container, c.tileSand.content, onClick = onEmail,
                enabled = !busy, modifier = Modifier.fillMaxWidth())
        } else {
            Text(stringResource(R.string.cloud_unavailable), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        TextButton(onClick = onGuest, enabled = !busy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.signin_continue_guest))
        }
    }
}

@Composable
private fun PermissionTile(icon: ImageVector, tile: TileColor, title: Int, body: Int, granted: Boolean, onAllow: () -> Unit) {
    val c = RaveTheme.colors
    BentoTile(tile.container, Modifier.fillMaxWidth(), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(BentoSquareShape).background(tile.content), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tile.container)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = tile.content)
                Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = tile.content.copy(alpha = 0.8f))
            }
            Spacer(Modifier.width(8.dp))
            if (granted) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = stringResource(R.string.cd_granted), tint = tile.content, modifier = Modifier.size(28.dp))
            } else {
                BentoChip(stringResource(R.string.action_allow), c.tileInk.container, c.tileInk.content, onClick = onAllow)
            }
        }
    }
}
