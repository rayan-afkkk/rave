package com.rave.app.health

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
                connected -> context.openHealthConnectSettings()
                else -> scope.launch {
                    runCatching { launcher.launch(source.permissionsToRequest()) }
                        .onFailure { context.openHealthConnectSettings() }
                }
            }
        },
    )
}

private fun Context.tryStart(intent: Intent): Boolean =
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

/** Health Connect's settings: built into Android 14+, a separate app before that. */
private fun Context.openHealthConnectSettings() {
    val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        "android.health.connect.action.HEALTH_HOME_SETTINGS"
    } else {
        "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
    }
    tryStart(Intent(action))
}
