package com.rave.app.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.ui.components.AppBackground
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.permissions.hasPermission
import com.rave.app.ui.permissions.notificationPermissions
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.theme.TileColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    fun finish(name: String, then: () -> Unit) {
        viewModelScope.launch {
            settings.update { it.copy(onboardingDone = true, displayName = name.trim().ifEmpty { it.displayName }) }
            then()
        }
    }
}

private data class Slide(val icon: ImageVector, val title: Int, val body: Int)

/** Three short slides, then name + permission explanations (nothing is asked without context). */
@Composable
fun OnboardingScreen(
    onFinished: (signIn: Boolean) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val slides = listOf(
        Slide(Icons.Rounded.Spa, R.string.onboarding_habits_title, R.string.onboarding_habits_body),
        Slide(Icons.AutoMirrored.Rounded.DirectionsRun, R.string.onboarding_gps_title, R.string.onboarding_gps_body),
        Slide(Icons.Rounded.Groups, R.string.onboarding_friends_title, R.string.onboarding_friends_body),
    )
    val pageCount = slides.size + 1
    val pager = rememberPagerState { pageCount }
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    val tiles = listOf(RaveTheme.colors.tilePeach, RaveTheme.colors.tileGreen, RaveTheme.colors.tilePink)

    AppBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                if (pager.currentPage < slides.size) {
                    TextButton(onClick = { scope.launch { pager.animateScrollToPage(slides.size) } }) { Text(stringResource(R.string.action_skip)) }
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                if (page < slides.size) {
                    val s = slides[page]
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(s.icon, stringResource(s.title), stringResource(s.body), tile = tiles[page])
                    }
                } else {
                    SetupPage(name) { name = it }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
                repeat(pageCount) { i ->
                    Box(Modifier.padding(3.dp).size(if (i == pager.currentPage) 10.dp else 7.dp).clip(CircleShape)
                        .background(if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
                }
            }
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                if (pager.currentPage < slides.size) {
                    PillButton(stringResource(R.string.action_next), onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                        modifier = Modifier.fillMaxWidth())
                } else {
                    PillButton(stringResource(R.string.onboarding_start_guest), onClick = { viewModel.finish(name) { onFinished(false) } },
                        modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { viewModel.finish(name) { onFinished(true) } }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(stringResource(R.string.onboarding_sign_in))
                    }
                }
            }
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
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.onboarding_setup_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        OutlinedTextField(
            value = name, onValueChange = { onName(it.take(40)) },
            label = { Text(stringResource(R.string.onboarding_name)) }, singleLine = true,
            shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.onboarding_permissions_intro), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (notificationPermissions.isNotEmpty()) {
            PermissionCard(Icons.Rounded.Notifications, RaveTheme.colors.tilePeach, R.string.onboarding_perm_notifications,
                R.string.onboarding_perm_notifications_body, notifGranted) { notifLauncher.launch(notificationPermissions.toTypedArray()) }
        }
        PermissionCard(Icons.Rounded.LocationOn, RaveTheme.colors.tileGreen, R.string.onboarding_perm_location,
            R.string.onboarding_perm_location_body, locationGranted) {
            locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        Text(stringResource(R.string.onboarding_perm_activity_note), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
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
                TextButton(onClick = onAllow) { Text(stringResource(R.string.action_allow), textAlign = TextAlign.Center) }
            }
        }
    }
}
