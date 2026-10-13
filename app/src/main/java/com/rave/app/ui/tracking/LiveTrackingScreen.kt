package com.rave.app.ui.tracking

import androidx.activity.compose.BackHandler
import com.rave.app.ui.theme.Dimens
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsNotFixed
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.rave.app.BuildConfig
import com.rave.app.R
import com.rave.app.domain.model.LatLngPoint
import com.rave.app.domain.util.Format
import com.rave.app.tracking.GpsQuality
import com.rave.app.tracking.TrackingState
import com.rave.app.tracking.TrackingStatus
import com.rave.app.ui.activityTypeRes
import com.rave.app.ui.components.ConfirmDialog
import com.rave.app.ui.components.PillProgress
import com.rave.app.ui.components.RouteCanvas
import com.rave.app.ui.components.StatItem
import com.rave.app.ui.paceUnitLabel
import com.rave.app.ui.permissions.LocationPermissionGate
import com.rave.app.ui.theme.PillShape
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.unitLabel

@Composable
fun LiveTrackingScreen(
    onBack: () -> Unit,
    onFinished: (Long) -> Unit,
    viewModel: LiveTrackingViewModel = hiltViewModel(),
) {
    LocationPermissionGate(onCancel = onBack) {
        LaunchedEffect(Unit) { viewModel.startIfNeeded() }
        var showTooShort by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            viewModel.events.collect { e ->
                when (e) {
                    is TrackingEvent.Finished -> onFinished(e.activityId)
                    TrackingEvent.TooShort -> showTooShort = true
                    TrackingEvent.Discarded -> onBack()
                }
            }
        }
        LiveTrackingContent(viewModel, onBack)
        if (showTooShort) {
            ConfirmDialog(
                title = stringResource(R.string.tracking_too_short_title),
                message = stringResource(R.string.tracking_too_short_body),
                confirmLabel = stringResource(R.string.action_discard),
                destructive = true,
                onConfirm = { showTooShort = false; viewModel.discard() },
                onDismiss = { showTooShort = false; viewModel.togglePause() },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LiveTrackingContent(viewModel: LiveTrackingViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var locked by rememberSaveable { mutableStateOf(false) }
    var follow by rememberSaveable { mutableStateOf(true) }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = locked) { /* swallow back while locked */ }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TrackingMap(state, follow, onUserMovedMap = { follow = false })

        // Top bar
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(onClick = onBack, enabled = !locked) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Spacer(Modifier.width(10.dp))
            Surface(shape = PillShape, color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(activityTypeRes(state.type.takeIf { state.isTracking } ?: viewModel.requestedType)),
                        style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(10.dp))
                    GpsChip(state.gps)
                }
            }
            Spacer(Modifier.weight(1f))
            RoundIconButton(onClick = { locked = true }, enabled = !locked) {
                Icon(Icons.Rounded.Lock, contentDescription = stringResource(R.string.tracking_lock))
            }
        }

        // Re-centre
        AnimatedVisibility(
            visible = !follow && !locked,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
        ) {
            SmallFloatingActionButton(onClick = { follow = true }, containerColor = MaterialTheme.colorScheme.surface) {
                Icon(Icons.Rounded.MyLocation, contentDescription = stringResource(R.string.tracking_recenter))
            }
        }

        // Stats sheet
        Surface(
            shape = MaterialTheme.shapes.large.copy(bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp),
                bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp)),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        ) {
            Column(Modifier.navigationBarsPadding().padding(20.dp)) {
                val unit = settings.unit
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                    Text(Format.distance(state.distanceM, unit), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(6.dp))
                    Text(unitLabel(unit), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp))
                    Spacer(Modifier.weight(1f))
                    if (state.status == TrackingStatus.PAUSED || state.status == TrackingStatus.AUTO_PAUSED) {
                        Text(
                            stringResource(if (state.status == TrackingStatus.PAUSED) R.string.tracking_paused else R.string.tracking_auto_paused),
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }
                state.goalM?.let { goal ->
                    val left = goal - state.distanceM
                    PillProgress(progress = if (goal <= 0) 1f else (state.distanceM / goal).toFloat())
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (left <= 0) stringResource(R.string.tracking_goal_reached)
                        else stringResource(R.string.tracking_to_go, Format.distance(left, unit), unitLabel(unit)) +
                            (state.goalHabitName?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem(Format.duration(state.elapsedMs / 1000), stringResource(R.string.stat_time))
                    StatItem(Format.pace(state.paceSPerKm, unit), stringResource(R.string.stat_pace), unit = paceUnitLabel(unit))
                    StatItem(state.calories.toString(), stringResource(R.string.stat_calories), unit = stringResource(R.string.unit_kcal))
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val active = state.status == TrackingStatus.ACTIVE
                    Surface(
                        onClick = viewModel::togglePause,
                        enabled = state.isTracking && !locked,
                        shape = PillShape,
                        color = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (active) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(if (active) R.string.action_pause else R.string.action_resume), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    val finishLabel = stringResource(R.string.tracking_finish_hint)
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .clip(PillShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .combinedClickable(
                                enabled = state.isTracking && !locked,
                                onClickLabel = finishLabel,
                                onClick = { confirmFinish = true },
                                onLongClick = { viewModel.finish() },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Flag, null, tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_finish), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }

        // Pocket lock overlay: swallows all touches; long-press to unlock.
        if (locked) {
            val unlockHint = stringResource(R.string.tracking_unlock_hint)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f))
                    .pointerInput(Unit) { detectTapGestures(onLongPress = { locked = false }) }
                    .semantics { contentDescription = unlockHint },
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = PillShape, color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LockOpen, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(unlockHint, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    if (confirmFinish) {
        ConfirmDialog(
            title = stringResource(R.string.tracking_finish_title),
            message = stringResource(R.string.tracking_finish_body),
            confirmLabel = stringResource(R.string.action_finish),
            onConfirm = { confirmFinish = false; viewModel.finish() },
            onDismiss = { confirmFinish = false },
        )
    }
}

@Composable
private fun TrackingMap(state: TrackingState, follow: Boolean, onUserMovedMap: () -> Unit) {
    val routeColor = MaterialTheme.colorScheme.primary
    val current = state.current
    val currentPt = current?.let { LatLngPoint(it.lat, it.lng) }
    val startPt = state.start

    val centerLat = current?.lat ?: startPt?.lat ?: 51.5074
    val centerLng = current?.lng ?: startPt?.lng ?: -0.1278

    com.rave.app.ui.components.TileMapView(
        centerLat = centerLat,
        centerLng = centerLng,
        initialZoom = 16f,
        segments = state.segments,
        currentLocation = currentPt,
        startLocation = startPt,
        guidePath = state.guide,
        followLocation = follow,
        interactive = true,
        routeColor = routeColor,
        onUserPanned = onUserMovedMap,
        modifier = Modifier.fillMaxSize()
    )
}

private fun List<LatLngPoint>.toLatLng(): List<LatLng> = map { LatLng(it.lat, it.lng) }

@Composable
private fun GpsChip(gps: GpsQuality) {
    val (icon, label, color) = when (gps) {
        GpsQuality.STRONG -> Triple(Icons.Rounded.GpsFixed, R.string.gps_strong, MaterialTheme.colorScheme.tertiary)
        GpsQuality.WEAK -> Triple(Icons.Rounded.GpsNotFixed, R.string.gps_weak, MaterialTheme.colorScheme.primary)
        GpsQuality.SEARCHING -> Triple(Icons.Rounded.GpsOff, R.string.gps_searching, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun RoundIconButton(onClick: () -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(48.dp)) {
        IconButton(onClick = onClick, enabled = enabled) { content() }
    }
}
