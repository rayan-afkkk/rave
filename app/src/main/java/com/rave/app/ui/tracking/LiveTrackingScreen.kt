package com.rave.app.ui.tracking

import androidx.activity.compose.BackHandler
import com.rave.app.ui.components.SmoothCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.LatLng
import com.rave.app.R
import com.rave.app.domain.model.LatLngPoint
import com.rave.app.domain.util.Format
import com.rave.app.tracking.GpsQuality
import com.rave.app.tracking.TrackingState
import com.rave.app.tracking.TrackingStatus
import com.rave.app.ui.activityTypeRes
import com.rave.app.ui.components.BentoChip
import com.rave.app.ui.components.BentoIconButton
import com.rave.app.ui.components.ConfirmDialog
import com.rave.app.ui.components.PillProgress
import com.rave.app.ui.permissions.LocationPermissionGate
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

@Composable
private fun LiveTrackingContent(viewModel: LiveTrackingViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var locked by rememberSaveable { mutableStateOf(false) }
    var follow by rememberSaveable { mutableStateOf(true) }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = locked) { /* swallow back while locked */ }
    val c = RaveTheme.colors
    val ink = c.tileInk

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TrackingMap(state, follow, onUserMovedMap = { follow = false })

        // Top controls floating on the map
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BentoIconButton(
                Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back),
                onClick = { if (!locked) onBack() }, container = ink.container, content = ink.content,
            )
            Spacer(Modifier.weight(1f))
            BentoIconButton(
                Icons.Rounded.Lock, stringResource(R.string.tracking_lock),
                onClick = { locked = true }, container = ink.container, content = ink.content,
            )
        }

        // Re-centre
        AnimatedVisibility(
            visible = !follow && !locked,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
        ) {
            BentoIconButton(
                Icons.Rounded.MyLocation, stringResource(R.string.tracking_recenter),
                onClick = { follow = true }, container = ink.container, content = ink.content,
            )
        }

        // Floating black stats card
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 14.dp)
                .clip(SheetShape)
                .background(ink.container)
                .padding(20.dp),
        ) {
            val unit = settings.unit
            val typeLabel = stringResource(activityTypeRes(state.type.takeIf { state.isTracking } ?: viewModel.requestedType))
            val paused = state.status == TrackingStatus.PAUSED || state.status == TrackingStatus.AUTO_PAUSED
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (paused) {
                    val pausedLabel = stringResource(if (state.status == TrackingStatus.PAUSED) R.string.tracking_paused else R.string.tracking_auto_paused)
                    BentoChip(
                        stringResource(R.string.btrack_paused_chip, pausedLabel, typeLabel),
                        container = c.inkRaised, content = ink.content,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                } else {
                    LiveChip(stringResource(R.string.btrack_live_chip, typeLabel))
                }
                Spacer(Modifier.weight(1f))
                GpsLabel(state.gps)
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 10.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    Format.distance(state.distanceM, unit), color = ink.content, maxLines = 1,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-3).sp, lineHeight = 60.sp,
                    ),
                )
                Text(
                    unitLabel(unit), color = c.navContent,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
                )
            }
            state.goalM?.let { goal ->
                val left = goal - state.distanceM
                Spacer(Modifier.height(12.dp))
                PillProgress(progress = if (goal <= 0) 1f else (state.distanceM / goal).toFloat(), color = c.highlight)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (left <= 0) stringResource(R.string.tracking_goal_reached)
                    else stringResource(R.string.tracking_to_go, Format.distance(left, unit), unitLabel(unit)) +
                        (state.goalHabitName?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = c.navContent,
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BentoMiniStat(Format.duration(state.elapsedMs / 1000), stringResource(R.string.stat_time), c.tilePink, Modifier.weight(1f))
                BentoMiniStat(bentoPace(state.paceSPerKm, unit), stringResource(R.string.stat_pace), c.tileBlue, Modifier.weight(1f))
                BentoMiniStat(state.calories.toString(), stringResource(R.string.unit_kcal), c.tilePeach, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val active = state.status == TrackingStatus.ACTIVE
                BentoPillAction(
                    stringResource(if (active) R.string.action_pause else R.string.action_resume),
                    container = c.inkRaised, content = ink.content,
                    onClick = viewModel::togglePause,
                    enabled = state.isTracking && !locked,
                    leading = if (active) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    modifier = Modifier.weight(1f),
                )
                BentoPillAction(
                    stringResource(R.string.action_finish),
                    container = c.cta.container, content = c.cta.content,
                    onClick = { confirmFinish = true },
                    onLongClick = { viewModel.finish() },
                    onClickLabel = stringResource(R.string.tracking_finish_hint),
                    enabled = state.isTracking && !locked,
                    trailing = Icons.Rounded.Flag,
                    modifier = Modifier.weight(1f),
                )
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
                BentoChip(unlockHint, container = ink.container, content = ink.content, icon = Icons.Rounded.LockOpen)
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
    // Route in ink; the current-position marker follows the route colour (TileMapView has no separate dot colour).
    val routeColor = RaveTheme.colors.tileInk.container
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

private val SheetShape = SmoothCornerShape(38.dp)

@Composable
private fun LiveChip(text: String) {
    val c = RaveTheme.colors
    BentoChip(text, container = c.highlight, content = c.onHighlight)
}

@Composable
private fun GpsLabel(gps: GpsQuality) {
    val c = RaveTheme.colors
    val (label, color) = when (gps) {
        GpsQuality.STRONG -> R.string.gps_strong to c.navContent
        GpsQuality.WEAK -> R.string.gps_weak to c.tileOrange.container
        GpsQuality.SEARCHING -> R.string.gps_searching to c.navContent
    }
    Text(stringResource(label), style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = color, maxLines = 1)
}
