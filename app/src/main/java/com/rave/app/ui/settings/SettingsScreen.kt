package com.rave.app.ui.settings

import android.Manifest
import com.rave.app.ui.theme.Dimens
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rave.app.BuildConfig
import com.rave.app.R
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.model.NotificationCategory
import com.rave.app.domain.model.ProfileVisibility
import com.rave.app.domain.model.ThemeMode
import com.rave.app.ui.components.ConfirmDialog
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.permissions.hasPermission
import com.rave.app.ui.permissions.openAppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPrivacyZones: () -> Unit,
    onSignIn: () -> Unit,
    onAccountGone: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state.settings
    val context = LocalContext.current
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }

    val exportedOk = stringResource(R.string.settings_export_done)
    val exportFailed = stringResource(R.string.settings_export_failed)
    val savedText = stringResource(R.string.settings_profile_saved)
    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is SettingsEvent.Exported -> Toast.makeText(context, if (e.ok) exportedOk else exportFailed, Toast.LENGTH_SHORT).show()
                SettingsEvent.Deleted, SettingsEvent.SignedOut -> onAccountGone()
                is SettingsEvent.Error -> Toast.makeText(context, e.message.ifBlank { exportFailed }, Toast.LENGTH_LONG).show()
                SettingsEvent.ProfileSaved -> Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::export)
    }

    // Auto-detect needs activity recognition, then "Allow all the time" location.
    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) context.openAppSettings()
        viewModel.setAutoDetect(true)
    }
    val recognitionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) dialog = DIALOG_BACKGROUND else dialog = DIALOG_RECOGNITION_DENIED
    }
    fun startAutoDetectFlow() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !context.hasPermission(Manifest.permission.ACTIVITY_RECOGNITION) ->
                dialog = DIALOG_RECOGNITION
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !context.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ->
                dialog = DIALOG_BACKGROUND
            else -> viewModel.setAutoDetect(true)
        }
    }

    RaveScreen(title = stringResource(R.string.settings_title), onBack = onBack) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
        ) {
            // Account
            SettingsSection(stringResource(R.string.settings_account)) {
                if (state.user != null) {
                    ValueRow(stringResource(R.string.settings_signed_in_as), state.user?.email ?: state.user?.name.orEmpty())
                    NavRow(stringResource(R.string.settings_edit_profile)) { dialog = DIALOG_PROFILE }
                    NavRow(stringResource(R.string.settings_phone)) { dialog = DIALOG_PHONE }
                } else {
                    NavRow(stringResource(R.string.settings_edit_name)) { dialog = DIALOG_PROFILE }
                    if (state.cloudAvailable) NavRow(stringResource(R.string.action_sign_in), onClick = onSignIn)
                }
            }

            // Appearance
            SettingsSection(stringResource(R.string.settings_appearance)) {
                Segmented(
                    listOf(ThemeMode.LIGHT to R.string.theme_light, ThemeMode.DARK to R.string.theme_dark, ThemeMode.SYSTEM to R.string.theme_system),
                    s.themeMode,
                ) { mode -> viewModel.update { it.copy(themeMode = mode) } }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.settings_units), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Segmented(listOf(DistanceUnit.KM to R.string.unit_km_long, DistanceUnit.MILES to R.string.unit_mi_long), s.unit) { u ->
                    viewModel.update { it.copy(unit = u) }
                }
            }

            // Body & goals
            SettingsSection(stringResource(R.string.settings_body)) {
                NumberRow(stringResource(R.string.settings_weight), s.weightKg, stringResource(R.string.unit_kg)) { v -> viewModel.update { it.copy(weightKg = v) } }
                NumberRow(stringResource(R.string.settings_height), s.heightCm, stringResource(R.string.unit_cm)) { v -> viewModel.update { it.copy(heightCm = v) } }
                NumberRow(stringResource(R.string.settings_daily_goal), s.dailyGoalKm, stringResource(R.string.unit_km)) { v -> viewModel.update { it.copy(dailyGoalKm = v) } }
                NumberRow(stringResource(R.string.settings_step_goal), s.dailyStepGoal.toFloat(), stringResource(R.string.unit_steps), decimals = 0) { v ->
                    viewModel.update { it.copy(dailyStepGoal = v.toInt().coerceIn(500, 100_000)) }
                }
            }

            // Tracking
            SettingsSection(stringResource(R.string.settings_tracking)) {
                SwitchRow(stringResource(R.string.settings_auto_pause), stringResource(R.string.settings_auto_pause_help), s.autoPause) { on ->
                    viewModel.update { it.copy(autoPause = on) }
                }
                SwitchRow(stringResource(R.string.settings_voice), stringResource(R.string.settings_voice_help), s.voiceFeedback) { on ->
                    viewModel.update { it.copy(voiceFeedback = on) }
                }
                SwitchRow(stringResource(R.string.settings_battery_saver), stringResource(R.string.settings_battery_saver_help), s.batterySaverGps) { on ->
                    viewModel.update { it.copy(batterySaverGps = on) }
                }
                SwitchRow(stringResource(R.string.settings_auto_detect), stringResource(R.string.settings_auto_detect_help), s.autoDetectWalks) { on ->
                    if (on) startAutoDetectFlow() else viewModel.setAutoDetect(false)
                }
            }

            // Privacy
            SettingsSection(stringResource(R.string.settings_privacy)) {
                NavRow(stringResource(R.string.privacy_title), onClick = onPrivacyZones)
                Text(stringResource(R.string.settings_visibility), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Segmented(
                    listOf(ProfileVisibility.FRIENDS to R.string.visibility_friends, ProfileVisibility.PRIVATE to R.string.visibility_private),
                    s.visibility,
                ) { v -> viewModel.setVisibility(v) }
            }

            // Notifications
            SettingsSection(stringResource(R.string.settings_notifications)) {
                listOf(
                    NotificationCategory.REMINDERS to R.string.notif_cat_reminders,
                    NotificationCategory.FRIENDS to R.string.notif_cat_friends,
                    NotificationCategory.COMPETITIONS to R.string.notif_cat_competitions,
                    NotificationCategory.AUTO_DETECT to R.string.notif_cat_auto_detect,
                ).forEach { (cat, label) ->
                    SwitchRow(stringResource(label), null, cat in s.notifications) { on -> viewModel.toggleNotification(cat, on) }
                }
            }

            // Data
            SettingsSection(stringResource(R.string.settings_data)) {
                NavRow(stringResource(R.string.settings_export)) { exportLauncher.launch("rave-export.zip") }
                if (state.user != null) NavRow(stringResource(R.string.settings_sign_out)) { viewModel.signOut() }
                NavRow(
                    stringResource(if (state.user != null) R.string.settings_delete_account else R.string.settings_delete_data),
                    destructive = true,
                ) { dialog = DIALOG_DELETE }
            }

            Text(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    when (dialog) {
        DIALOG_DELETE -> ConfirmDialog(
            title = stringResource(R.string.settings_delete_title),
            message = stringResource(if (state.user != null) R.string.settings_delete_account_body else R.string.settings_delete_data_body),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { dialog = null; viewModel.deleteAccount() },
            onDismiss = { dialog = null },
        )
        DIALOG_RECOGNITION -> ConfirmDialog(
            title = stringResource(R.string.permission_activity_title),
            message = stringResource(R.string.permission_activity_body),
            confirmLabel = stringResource(R.string.action_allow),
            onConfirm = { dialog = null; recognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
            onDismiss = { dialog = null },
        )
        DIALOG_RECOGNITION_DENIED -> ConfirmDialog(
            title = stringResource(R.string.permission_activity_title),
            message = stringResource(R.string.permission_denied_body, stringResource(R.string.permission_activity_body)),
            confirmLabel = stringResource(R.string.action_open_settings),
            onConfirm = { dialog = null; context.openAppSettings() },
            onDismiss = { dialog = null },
        )
        DIALOG_BACKGROUND -> ConfirmDialog(
            title = stringResource(R.string.permission_background_title),
            message = stringResource(R.string.permission_background_body),
            confirmLabel = stringResource(R.string.action_continue),
            onConfirm = {
                dialog = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                else viewModel.setAutoDetect(true)
            },
            onDismiss = { dialog = null; viewModel.setAutoDetect(true) },
        )
        DIALOG_PROFILE -> ProfileDialog(
            name = state.profile?.name ?: s.displayName,
            username = state.profile?.username.orEmpty(),
            showUsername = state.user != null,
            onSave = { n, u -> dialog = null; viewModel.saveProfile(n, u) },
            onDismiss = { dialog = null },
        )
        DIALOG_PHONE -> PhoneDialog(onSave = { dialog = null; viewModel.setPhoneNumber(it) }, onDismiss = { dialog = null })
    }
}

private const val DIALOG_DELETE = "delete"
private const val DIALOG_RECOGNITION = "recognition"
private const val DIALOG_RECOGNITION_DENIED = "recognition_denied"
private const val DIALOG_BACKGROUND = "background"
private const val DIALOG_PROFILE = "profile"
private const val DIALOG_PHONE = "phone"

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp).semantics { heading() })
        SectionCard { content() }
    }
}

@Composable
private fun SwitchRow(title: String, help: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            help?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun NavRow(title: String, destructive: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ValueRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun NumberRow(title: String, value: Float, unit: String, decimals: Int = 1, onSave: (Float) -> Unit) {
    val pattern = "%.${decimals}f"
    var editing by rememberSaveable { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { editing = true },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text("${pattern.format(value)} $unit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
    if (editing) {
        var text by rememberSaveable { mutableStateOf(pattern.format(value)) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(title) },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' || it == ',' }.take(6) },
                    suffix = { Text(unit) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    text.replace(',', '.').toFloatOrNull()?.takeIf { it > 0 }?.let(onSave)
                    editing = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(R.string.action_cancel)) } },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Segmented(options: List<Pair<T, Int>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                label = { Text(stringResource(label)) },
            )
        }
    }
}

@Composable
private fun ProfileDialog(name: String, username: String, showUsername: Boolean, onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var n by rememberSaveable { mutableStateOf(name) }
    var u by rememberSaveable { mutableStateOf(username) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_edit_profile)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(n, { n = it.take(40) }, label = { Text(stringResource(R.string.settings_name)) }, singleLine = true)
                if (showUsername) {
                    OutlinedTextField(u, { u = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(20) },
                        label = { Text(stringResource(R.string.settings_username)) }, prefix = { Text("@") }, singleLine = true,
                        supportingText = { Text(stringResource(R.string.settings_username_help)) })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(n, u) }, enabled = n.isNotBlank()) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun PhoneDialog(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var number by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Phone, null) },
        title = { Text(stringResource(R.string.settings_phone)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.settings_phone_help), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(number, { number = it.take(20) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            }
        },
        confirmButton = { TextButton(onClick = { onSave(number) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
