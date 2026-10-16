package com.rave.app.ui.permissions

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.rave.app.R
import com.rave.app.ui.components.IconTile
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.theme.RaveTheme

fun Context.notificationsAllowed(): Boolean = NotificationManagerCompat.from(this).areNotificationsEnabled()

fun Context.exactAlarmsAllowed(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

/** Asks for notification permission the first time reminders are switched on. */
@Composable
fun rememberNotificationRequest(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/**
 * Explains (and fixes) the two things that stop reminders from showing: notifications switched
 * off for the app, and Android 12+ not allowing exact alarms. Shows nothing when all is well.
 */
@Composable
fun ReminderHealth(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var notifications by remember { mutableStateOf(context.notificationsAllowed()) }
    var exact by remember { mutableStateOf(context.exactAlarmsAllowed()) }
    LifecycleResumeEffect(Unit) {
        notifications = context.notificationsAllowed()
        exact = context.exactAlarmsAllowed()
        onPauseOrDispose { }
    }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifications = context.notificationsAllowed() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!notifications) {
            Fix(Icons.Rounded.NotificationsOff, stringResource(R.string.reminders_notif_off), null, stringResource(R.string.reminders_notif_action)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                    request.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
        }
        if (!exact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Fix(Icons.Rounded.Alarm, stringResource(R.string.reminders_exact_title), stringResource(R.string.reminders_exact_body),
                stringResource(R.string.reminders_exact_action)) {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.onFailure { context.openAppSettings() }
            }
        }
    }
}

@Composable
private fun Fix(icon: ImageVector, title: String, body: String?, action: String, onClick: () -> Unit) {
    SectionCard(Modifier.fillMaxWidth(), color = RaveTheme.colors.tilePeach.container) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, RaveTheme.colors.tilePeach)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                body?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            TextButton(onClick = onClick) { Text(action) }
        }
    }
}
