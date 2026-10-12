package com.rave.app.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.rave.app.R
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.theme.RaveTheme

fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

fun Context.isLocationEnabled(): Boolean {
    val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) lm.isLocationEnabled
    else lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

/** Notification permission only exists on Android 13+. */
val notificationPermissions: List<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()

/** A friendly full-screen explanation shown before (or after denying) a permission. */
@Composable
fun PermissionExplainer(
    icon: ImageVector,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyState(icon = icon, title = title, message = body, tile = RaveTheme.colors.tileGreen)
        Spacer(Modifier.height(8.dp))
        PillButton(primaryLabel, onClick = onPrimary, modifier = Modifier.fillMaxWidth())
        if (secondaryLabel != null && onSecondary != null) {
            TextButton(onClick = onSecondary) { Text(secondaryLabel) }
        }
    }
}

/**
 * Generic gate: explains why [required] is needed, asks for [request] (which should include
 * [required]), and offers "Open settings" once the user has denied permanently.
 */
@Composable
fun PermissionGate(
    required: String,
    request: List<String>,
    icon: ImageVector,
    title: String,
    body: String,
    onCancel: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasPermission(required)) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = context.hasPermission(required)
        asked = true
    }
    LifecycleResumeEffect(Unit) {
        granted = context.hasPermission(required)
        onPauseOrDispose { }
    }
    if (granted) {
        content()
        return
    }
    val activity = context.findActivity()
    val permanentlyDenied = asked && activity?.shouldShowRequestPermissionRationale(required) == false
    PermissionExplainer(
        icon = icon,
        title = title,
        body = if (permanentlyDenied) stringResource(R.string.permission_denied_body, body) else body,
        primaryLabel = stringResource(if (permanentlyDenied) R.string.action_open_settings else R.string.action_allow),
        onPrimary = { if (permanentlyDenied) context.openAppSettings() else launcher.launch(request.toTypedArray()) },
        secondaryLabel = onCancel?.let { stringResource(R.string.action_not_now) },
        onSecondary = onCancel,
    )
}

/** Precise location (while using the app) + location services switched on. Also asks for notifications. */
@Composable
fun LocationPermissionGate(onCancel: () -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    PermissionGate(
        required = Manifest.permission.ACCESS_FINE_LOCATION,
        request = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) + notificationPermissions,
        icon = Icons.Rounded.LocationOn,
        title = stringResource(R.string.permission_location_title),
        body = stringResource(R.string.permission_location_body),
        onCancel = onCancel,
    ) {
        var enabled by remember { mutableStateOf(context.isLocationEnabled()) }
        LifecycleResumeEffect(Unit) {
            enabled = context.isLocationEnabled()
            onPauseOrDispose { }
        }
        if (enabled) {
            content()
        } else {
            PermissionExplainer(
                icon = Icons.Rounded.LocationOff,
                title = stringResource(R.string.permission_location_off_title),
                body = stringResource(R.string.permission_location_off_body),
                primaryLabel = stringResource(R.string.action_open_settings),
                onPrimary = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                secondaryLabel = stringResource(R.string.action_not_now),
                onSecondary = onCancel,
            )
        }
    }
}
