package com.rave.app.health

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch

/** Whether a smartwatch is connected through Health Connect, and how to connect or manage it. */
class WatchConnectState(
    val supported: Boolean,
    val connected: Boolean,
    val connect: () -> Unit,
)

@Composable
fun rememberWatchConnect(): WatchConnectState {
    val context = LocalContext.current
    val source = remember { HealthConnectSource(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var availability by remember { mutableStateOf(source.availability()) }
    var connected by remember { mutableStateOf(false) }
    fun refresh() {
        availability = source.availability()
        scope.launch { connected = source.hasPermissions() }
    }
    val launcher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { refresh() }
    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose { }
    }
    return WatchConnectState(
        supported = availability != HealthConnectSource.Availability.UNAVAILABLE,
        connected = connected,
        connect = {
            when {
                availability == HealthConnectSource.Availability.NEEDS_UPDATE -> context.tryStart(source.installIntent())
                connected -> context.tryStart(Intent(HealthConnectClient.getHealthConnectSettingsAction()))
                else -> runCatching { launcher.launch(HealthConnectSource.PERMISSIONS) }
                    .onFailure { context.tryStart(Intent(HealthConnectClient.getHealthConnectSettingsAction())) }
            }
        },
    )
}

private fun Context.tryStart(intent: Intent) {
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
