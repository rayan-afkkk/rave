package com.rave.app.ui.habits

import androidx.annotation.StringRes
import com.rave.app.ui.theme.Dimens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rave.app.R
import com.rave.app.domain.model.ActivityGoalUnit
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.HabitColor
import com.rave.app.domain.model.HabitType
import com.rave.app.domain.model.ScheduleType
import com.rave.app.ui.components.ConfirmDialog
import com.rave.app.ui.components.HabitIcons
import com.rave.app.ui.components.IconTile
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.PillChip
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.tile
import com.rave.app.ui.unitLabel
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private data class HabitTemplate(@StringRes val nameRes: Int, @StringRes val unitRes: Int?, val form: HabitForm)

private val TEMPLATES = listOf(
    HabitTemplate(R.string.template_water, R.string.template_water_unit,
        HabitForm(icon = "water", color = HabitColor.BLUE, type = HabitType.COUNTER, counterTarget = "8")),
    HabitTemplate(R.string.template_walk, null,
        HabitForm(icon = "walk", color = HabitColor.GREEN, type = HabitType.ACTIVITY, activityGoal = "2")),
    HabitTemplate(R.string.template_meditate, null,
        HabitForm(icon = "meditate", color = HabitColor.PINK, type = HabitType.TIMED, timedMinutes = "10")),
    HabitTemplate(R.string.template_read, null,
        HabitForm(icon = "read", color = HabitColor.PEACH, type = HabitType.TIMED, timedMinutes = "20")),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditHabitScreen(
    onDone: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: AddEditHabitViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    var showArchive by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val askNotifications = com.rave.app.ui.permissions.rememberNotificationRequest()

    RaveScreen(
        title = stringResource(if (state.isEdit) R.string.habit_edit_title else R.string.habit_new_title),
        onBack = onDone,
    ) {
        if (state.loading) {
            LoadingState()
            return@RaveScreen
        }
        val form = state.form
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        )

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!state.isEdit) {
                Text(stringResource(R.string.habit_templates), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TEMPLATES) { t ->
                        val name = stringResource(t.nameRes)
                        val unit = t.unitRes?.let { stringResource(it) }.orEmpty()
                        PillChip(
                            selected = form.name == name,
                            label = name,
                            icon = HabitIcons.get(t.form.icon),
                            onClick = { viewModel.edit { t.form.copy(name = name, counterUnit = unit) } },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = form.name,
                onValueChange = { v -> viewModel.edit { it.copy(name = v.take(40)) } },
                label = { Text(stringResource(R.string.habit_name)) },
                isError = state.nameError,
                supportingText = if (state.nameError) { { Text(stringResource(R.string.habit_name_error)) } } else null,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            // Icon + colour
            SectionCard {
                Text(stringResource(R.string.habit_icon), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(HabitIcons.all) { (key, icon) ->
                        val selected = form.icon == key
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium) else Modifier)
                                .clickable(role = Role.RadioButton) { viewModel.edit { it.copy(icon = key) } }
                                .semantics { this.selected = selected; contentDescription = key },
                            contentAlignment = Alignment.Center,
                        ) { IconTile(icon, form.color.tile()) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.habit_color), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HabitColor.entries.forEach { c ->
                        val selected = form.color == c
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(c.tile().container)
                                .border(if (selected) 3.dp else 0.dp, c.tile().content, CircleShape)
                                .clickable(role = Role.RadioButton) { viewModel.edit { it.copy(color = c) } }
                                .semantics { this.selected = selected; contentDescription = c.name.lowercase() },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Rounded.Check, null, tint = c.tile().content)
                        }
                    }
                }
            }

            // Type
            SectionCard {
                Text(stringResource(R.string.habit_type), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                val types = listOf(
                    HabitType.CHECKBOX to R.string.habit_type_check,
                    HabitType.COUNTER to R.string.habit_type_counter,
                    HabitType.TIMED to R.string.habit_type_timed,
                    HabitType.ACTIVITY to R.string.habit_type_activity,
                    HabitType.WORKOUT to R.string.habit_type_workout,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { (type, label) ->
                        PillChip(form.type == type, stringResource(label), { viewModel.edit { it.copy(type = type) } })
                    }
                }
                Spacer(Modifier.height(12.dp))
                when (form.type) {
                    HabitType.WORKOUT -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(form.workoutSets, { v -> viewModel.edit { it.copy(workoutSets = v) } },
                            R.string.habit_workout_min_sets, state.goalError, Modifier.fillMaxWidth())
                        Text(stringResource(R.string.habit_type_workout_help), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HabitType.CHECKBOX -> Text(stringResource(R.string.habit_type_check_help), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HabitType.COUNTER -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumberField(form.counterTarget, { v -> viewModel.edit { it.copy(counterTarget = v) } }, R.string.habit_target,
                            state.goalError, Modifier.weight(1f))
                        OutlinedTextField(
                            value = form.counterUnit,
                            onValueChange = { v -> viewModel.edit { it.copy(counterUnit = v.take(16)) } },
                            label = { Text(stringResource(R.string.habit_unit)) },
                            singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.weight(1f),
                        )
                    }
                    HabitType.TIMED -> NumberField(form.timedMinutes, { v -> viewModel.edit { it.copy(timedMinutes = v) } },
                        R.string.habit_minutes, state.goalError, Modifier.fillMaxWidth())
                    HabitType.ACTIVITY -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PillChip(form.activityType == ActivityType.WALK, stringResource(R.string.activity_walk),
                                { viewModel.edit { it.copy(activityType = ActivityType.WALK) } })
                            PillChip(form.activityType == ActivityType.RUN, stringResource(R.string.activity_run),
                                { viewModel.edit { it.copy(activityType = ActivityType.RUN) } })
                            Spacer(Modifier.width(8.dp))
                            PillChip(form.activityGoalUnit == ActivityGoalUnit.METERS, unitLabel(state.unit),
                                { viewModel.edit { it.copy(activityGoalUnit = ActivityGoalUnit.METERS) } })
                            PillChip(form.activityGoalUnit == ActivityGoalUnit.MINUTES, stringResource(R.string.unit_min),
                                { viewModel.edit { it.copy(activityGoalUnit = ActivityGoalUnit.MINUTES) } })
                        }
                        NumberField(form.activityGoal, { v -> viewModel.edit { it.copy(activityGoal = v) } },
                            R.string.habit_goal, state.goalError, Modifier.fillMaxWidth(), decimal = true)
                        Text(stringResource(R.string.habit_type_activity_help), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Schedule
            SectionCard {
                Text(stringResource(R.string.habit_schedule), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillChip(form.scheduleType == ScheduleType.EVERY_DAY, stringResource(R.string.schedule_every_day),
                        { viewModel.edit { it.copy(scheduleType = ScheduleType.EVERY_DAY) } })
                    PillChip(form.scheduleType == ScheduleType.SPECIFIC_DAYS, stringResource(R.string.schedule_days),
                        { viewModel.edit { it.copy(scheduleType = ScheduleType.SPECIFIC_DAYS) } })
                    PillChip(form.scheduleType == ScheduleType.TIMES_PER_WEEK, stringResource(R.string.schedule_weekly),
                        { viewModel.edit { it.copy(scheduleType = ScheduleType.TIMES_PER_WEEK) } })
                }
                Spacer(Modifier.height(12.dp))
                when (form.scheduleType) {
                    ScheduleType.SPECIFIC_DAYS -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.entries.forEach { day ->
                            val on = day in form.days
                            PillChip(on, day.getDisplayName(TextStyle.SHORT, Locale.getDefault()), {
                                viewModel.edit { it.copy(days = if (on) it.days - day else it.days + day) }
                            })
                        }
                    }
                    ScheduleType.TIMES_PER_WEEK -> Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.edit { it.copy(timesPerWeek = (it.timesPerWeek - 1).coerceAtLeast(1)) } }) {
                            Text("−", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(stringResource(R.string.schedule_times_per_week, form.timesPerWeek), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { viewModel.edit { it.copy(timesPerWeek = (it.timesPerWeek + 1).coerceAtMost(6)) } }) {
                            Text("+", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    ScheduleType.EVERY_DAY -> Unit
                }
            }

            // Reminders
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.habit_reminders), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Switch(
                        checked = form.remindersEnabled,
                        onCheckedChange = { on ->
                            viewModel.edit { it.copy(remindersEnabled = on) }
                            if (on) askNotifications()
                            if (on && form.reminders.isEmpty()) showTimePicker = true
                        },
                    )
                }
                if (form.remindersEnabled) {
                    com.rave.app.ui.permissions.ReminderHealth(Modifier.padding(vertical = 6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        form.reminders.forEach { t ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(t.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))) },
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.edit { it.copy(reminders = it.reminders - t) } }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_remove), Modifier.size(18.dp))
                                    }
                                },
                            )
                        }
                        TextButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Rounded.Add, null)
                            Text(stringResource(R.string.habit_add_time))
                        }
                    }
                }
            }

            // Shared streak
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.People, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.habit_share_title), style = MaterialTheme.typography.titleSmall)
                        Text(
                            when {
                                state.sharedWith != null -> stringResource(R.string.habit_shared_with, state.sharedWith!!)
                                !state.signedIn -> stringResource(R.string.habit_share_sign_in)
                                state.friends.isEmpty() -> stringResource(R.string.habit_share_no_friends)
                                else -> stringResource(R.string.habit_share_help)
                            },
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (state.sharedWith == null) {
                    Spacer(Modifier.height(8.dp))
                    if (!state.signedIn) {
                        TextButton(onClick = onSignIn) { Text(stringResource(R.string.action_sign_in)) }
                    } else if (state.friends.isNotEmpty()) {
                        var open by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { open = true }) {
                                Text(form.shareWith?.name ?: stringResource(R.string.habit_share_pick))
                            }
                            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.habit_share_nobody)) },
                                    onClick = { viewModel.edit { it.copy(shareWith = null) }; open = false })
                                state.friends.forEach { f ->
                                    DropdownMenuItem(text = { Text(f.name) }, onClick = { viewModel.edit { it.copy(shareWith = f) }; open = false })
                                }
                            }
                        }
                    }
                }
            }

            if (state.goalError) {
                Text(stringResource(R.string.habit_goal_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            PillButton(
                text = stringResource(R.string.action_save),
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.isEdit) {
                TextButton(onClick = { showArchive = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.habit_archive), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showArchive) {
        ConfirmDialog(
            title = stringResource(R.string.habit_archive_title),
            message = stringResource(R.string.habit_archive_message),
            confirmLabel = stringResource(R.string.habit_archive),
            destructive = true,
            onConfirm = { showArchive = false; viewModel.archive() },
            onDismiss = { showArchive = false },
        )
    }
    if (showTimePicker) {
        ReminderTimeDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { t ->
                showTimePicker = false
                viewModel.edit { it.copy(reminders = (it.reminders + t).distinct().sorted(), remindersEnabled = true) }
            },
        )
    }
}

@Composable
private fun NumberField(
    value: String,
    onChange: (String) -> Unit,
    @StringRes label: Int,
    error: Boolean,
    modifier: Modifier,
    decimal: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }.take(6)) },
        label = { Text(stringResource(label)) },
        isError = error,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val pickerState = rememberTimePickerState(initialHour = 8, initialMinute = 0)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) { Text(stringResource(R.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        title = { Text(stringResource(R.string.habit_add_time)) },
        text = { TimePicker(state = pickerState) },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
