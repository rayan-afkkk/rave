package com.rave.app.ui.habits

import androidx.annotation.StringRes
import com.rave.app.ui.components.SmoothCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rave.app.R
import com.rave.app.domain.model.ActivityGoalUnit
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.HabitColor
import com.rave.app.domain.model.HabitType
import com.rave.app.domain.model.ScheduleType
import com.rave.app.ui.components.AppBackground
import com.rave.app.ui.components.BentoButton
import com.rave.app.ui.components.BentoChip
import com.rave.app.ui.components.BentoIconButton
import com.rave.app.ui.components.BentoKicker
import com.rave.app.ui.components.BentoOutlinedCard
import com.rave.app.ui.components.BentoTile
import com.rave.app.ui.components.BentoTileShape
import com.rave.app.ui.components.BentoToggleChip
import com.rave.app.ui.components.ConfirmDialog
import com.rave.app.ui.components.HabitIcons
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.tile
import com.rave.app.ui.theme.Dimens
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.unitLabel
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/*
 * Bento Pop habit editor: close / title / check bar, a habit-colour name tile with the icon,
 * colour swatches, goal-type chips, a sand stepper tile, day toggles, a sky reminder tile
 * and a full-width black save button.
 */

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

private val SwatchShape = SmoothCornerShape(14.dp)
private val IconTileShape = SmoothCornerShape(22.dp)
private val DayShape = SmoothCornerShape(14.dp)

@Composable
private fun bigStyle(size: TextUnit): ComposeTextStyle = MaterialTheme.typography.displaySmall.copy(
    fontSize = size, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.04f * size.value).sp, lineHeight = (size.value * 1.1f).sp,
)

private fun stepInt(value: String, delta: Int, min: Int = 1): String =
    ((value.toIntOrNull() ?: 0) + delta).coerceIn(min, 999_999).toString()

private fun stepDecimal(value: String, delta: Double): String {
    val v = (value.replace(',', '.').toDoubleOrNull() ?: 0.0) + delta
    val next = v.coerceAtLeast(kotlin.math.abs(delta))
    return if (next % 1.0 == 0.0) next.toLong().toString() else String.format(Locale.US, "%.1f", next)
}

@OptIn(ExperimentalLayoutApi::class)
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
    /** True when the picker replaces the first reminder instead of adding one. */
    var replaceFirst by rememberSaveable { mutableStateOf(false) }
    var showIcons by rememberSaveable { mutableStateOf(false) }
    val askNotifications = com.rave.app.ui.permissions.rememberNotificationRequest()
    val c = RaveTheme.colors
    val onBg = MaterialTheme.colorScheme.onBackground

    AppBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Top bar: close · title · save
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDone) { Icon(Icons.Rounded.Close, stringResource(R.string.action_close), tint = onBg) }
                Text(
                    stringResource(if (state.isEdit) R.string.habit_edit_title else R.string.habit_new_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = onBg, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                IconButton(onClick = viewModel::save, enabled = !state.loading && !state.saving) {
                    Icon(Icons.Rounded.Check, stringResource(R.string.action_save), tint = onBg)
                }
            }
            if (state.loading) {
                LoadingState()
                return@Column
            }
            val form = state.form
            val habitTile = form.color.tile()
            val sand = c.tileSand
            val sky = c.tileBlue

            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = Dimens.ScreenPadding, vertical = 4.dp),
            ) {
                // Name tile
                Row(
                    Modifier.fillMaxWidth().clip(BentoTileShape).background(habitTile.container).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val changeIcon = stringResource(R.string.bprofile_change_icon)
                    Box(
                        Modifier.size(64.dp).clip(IconTileShape).background(c.tileInk.container)
                            .clickable(role = Role.Button) { showIcons = true }
                            .semantics { contentDescription = changeIcon },
                        contentAlignment = Alignment.Center,
                    ) { Icon(HabitIcons.get(form.icon), null, tint = habitTile.container, modifier = Modifier.size(36.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        BentoKicker(stringResource(R.string.habit_name), habitTile.content)
                        val nameStyle = bigStyle(26.sp).copy(color = habitTile.content)
                        Box {
                            if (form.name.isEmpty()) {
                                Text(stringResource(R.string.bprofile_name_hint), style = nameStyle, color = habitTile.content.copy(alpha = 0.4f), maxLines = 1)
                            }
                            BasicTextField(
                                value = form.name,
                                onValueChange = { v -> viewModel.edit { it.copy(name = v.take(40)) } },
                                singleLine = true,
                                textStyle = nameStyle,
                                cursorBrush = SolidColor(habitTile.content),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                if (state.nameError) {
                    Text(stringResource(R.string.habit_name_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp))
                }

                // Templates (new habits only)
                if (!state.isEdit) {
                    SectionKicker(stringResource(R.string.habit_templates))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TEMPLATES) { t ->
                            val name = stringResource(t.nameRes)
                            val unit = t.unitRes?.let { stringResource(it) }.orEmpty()
                            val selected = form.name == name
                            BentoChip(
                                name,
                                container = if (selected) c.tileInk.container else sand.container,
                                content = if (selected) c.tileInk.content else sand.content,
                                icon = HabitIcons.get(t.form.icon),
                                modifier = Modifier.height(40.dp),
                                onClick = { viewModel.edit { t.form.copy(name = name, counterUnit = unit) } },
                            )
                        }
                    }
                }

                // Colour
                SectionKicker(stringResource(R.string.habit_color))
                Row(Modifier.padding(start = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HabitColor.entries.forEach { hc ->
                        val selected = form.color == hc
                        Box(
                            Modifier
                                .size(40.dp)
                                .drawBehind {
                                    if (selected) {
                                        val stroke = 3.dp.toPx()
                                        val o = 3.dp.toPx() + stroke / 2f
                                        drawRoundRect(
                                            color = onBg,
                                            topLeft = Offset(-o, -o),
                                            size = Size(size.width + 2 * o, size.height + 2 * o),
                                            cornerRadius = CornerRadius(14.dp.toPx() + o),
                                            style = Stroke(stroke),
                                        )
                                    }
                                }
                                .clip(SwatchShape)
                                .background(hc.tile().container)
                                .clickable(role = Role.RadioButton) { viewModel.edit { it.copy(color = hc) } }
                                .semantics { this.selected = selected; contentDescription = hc.name.lowercase(Locale.ROOT) },
                        )
                    }
                }

                // Goal type
                SectionKicker(stringResource(R.string.bprofile_goal_type), top = 18.dp)
                val types = listOf(
                    HabitType.CHECKBOX to R.string.habit_type_check,
                    HabitType.TIMED to R.string.habit_type_timed,
                    HabitType.COUNTER to R.string.bprofile_count,
                    HabitType.ACTIVITY to R.string.habit_type_activity,
                    HabitType.WORKOUT to R.string.habit_type_workout,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { (type, label) ->
                        BentoToggleChip(stringResource(label), form.type == type, { viewModel.edit { it.copy(type = type) } })
                    }
                }
                Spacer(Modifier.height(12.dp))
                when (form.type) {
                    HabitType.CHECKBOX -> HelpText(stringResource(R.string.habit_type_check_help))
                    HabitType.TIMED -> StepperTile(
                        kicker = stringResource(R.string.comp_duration),
                        value = form.timedMinutes, suffix = stringResource(R.string.unit_min), error = state.goalError,
                        onValueChange = { v -> viewModel.edit { it.copy(timedMinutes = v) } },
                        onMinus = { viewModel.edit { it.copy(timedMinutes = stepInt(it.timedMinutes, -1)) } },
                        onPlus = { viewModel.edit { it.copy(timedMinutes = stepInt(it.timedMinutes, 1)) } },
                    )
                    HabitType.COUNTER -> StepperTile(
                        kicker = stringResource(R.string.bprofile_count),
                        value = form.counterTarget, suffix = form.counterUnit, error = state.goalError,
                        onValueChange = { v -> viewModel.edit { it.copy(counterTarget = v) } },
                        onMinus = { viewModel.edit { it.copy(counterTarget = stepInt(it.counterTarget, -1)) } },
                        onPlus = { viewModel.edit { it.copy(counterTarget = stepInt(it.counterTarget, 1)) } },
                    ) {
                        Spacer(Modifier.height(10.dp))
                        InlineField(
                            value = form.counterUnit, hint = stringResource(R.string.habit_unit),
                            onValueChange = { v -> viewModel.edit { it.copy(counterUnit = v.take(16)) } },
                        )
                    }
                    HabitType.WORKOUT -> {
                        StepperTile(
                            kicker = stringResource(R.string.habit_workout_min_sets),
                            value = form.workoutSets, suffix = "", error = state.goalError,
                            onValueChange = { v -> viewModel.edit { it.copy(workoutSets = v) } },
                            onMinus = { viewModel.edit { it.copy(workoutSets = stepInt(it.workoutSets, -1)) } },
                            onPlus = { viewModel.edit { it.copy(workoutSets = stepInt(it.workoutSets, 1)) } },
                        )
                        HelpText(stringResource(R.string.habit_type_workout_help), Modifier.padding(top = 8.dp))
                    }
                    HabitType.ACTIVITY -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BentoToggleChip(stringResource(R.string.activity_walk), form.activityType == ActivityType.WALK,
                                { viewModel.edit { it.copy(activityType = ActivityType.WALK) } })
                            BentoToggleChip(stringResource(R.string.activity_run), form.activityType == ActivityType.RUN,
                                { viewModel.edit { it.copy(activityType = ActivityType.RUN) } })
                            Spacer(Modifier.width(4.dp))
                            BentoToggleChip(unitLabel(state.unit), form.activityGoalUnit == ActivityGoalUnit.METERS,
                                { viewModel.edit { it.copy(activityGoalUnit = ActivityGoalUnit.METERS) } })
                            BentoToggleChip(stringResource(R.string.unit_min), form.activityGoalUnit == ActivityGoalUnit.MINUTES,
                                { viewModel.edit { it.copy(activityGoalUnit = ActivityGoalUnit.MINUTES) } })
                        }
                        Spacer(Modifier.height(12.dp))
                        val meters = form.activityGoalUnit == ActivityGoalUnit.METERS
                        val step = if (meters) 0.5 else 5.0
                        StepperTile(
                            kicker = stringResource(R.string.habit_goal),
                            value = form.activityGoal,
                            suffix = if (meters) unitLabel(state.unit) else stringResource(R.string.unit_min),
                            error = state.goalError, decimal = true,
                            onValueChange = { v -> viewModel.edit { it.copy(activityGoal = v) } },
                            onMinus = { viewModel.edit { it.copy(activityGoal = stepDecimal(it.activityGoal, -step)) } },
                            onPlus = { viewModel.edit { it.copy(activityGoal = stepDecimal(it.activityGoal, step)) } },
                        )
                        HelpText(stringResource(R.string.habit_type_activity_help), Modifier.padding(top = 8.dp))
                    }
                }

                // Days / weekly
                val weekly = form.scheduleType == ScheduleType.TIMES_PER_WEEK
                Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    BentoKicker(stringResource(R.string.bprofile_days), onBg, Modifier.weight(1f))
                    Text(
                        stringResource(if (weekly) R.string.schedule_days else R.string.schedule_weekly),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = onBg,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(role = Role.Button) {
                            viewModel.edit {
                                if (weekly) {
                                    val days = it.days.ifEmpty { DayOfWeek.entries.toSet() }
                                    it.copy(days = days, scheduleType = if (days.size == 7) ScheduleType.EVERY_DAY else ScheduleType.SPECIFIC_DAYS)
                                } else {
                                    it.copy(scheduleType = ScheduleType.TIMES_PER_WEEK)
                                }
                            }
                        }.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                if (weekly) {
                    BentoTile(sand.container, Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.schedule_times_per_week, form.timesPerWeek), style = bigStyle(28.sp), color = sand.content,
                                modifier = Modifier.weight(1f), maxLines = 1,
                            )
                            StepperButtons(
                                onMinus = { viewModel.edit { it.copy(timesPerWeek = (it.timesPerWeek - 1).coerceAtLeast(1)) } },
                                onPlus = { viewModel.edit { it.copy(timesPerWeek = (it.timesPerWeek + 1).coerceAtMost(6)) } },
                            )
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val active = if (form.scheduleType == ScheduleType.EVERY_DAY) DayOfWeek.entries.toSet() else form.days
                        DayOfWeek.entries.forEach { day ->
                            val on = day in active
                            val full = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                            Box(
                                Modifier.weight(1f).height(44.dp).clip(DayShape)
                                    .background(if (on) c.tileInk.container else sand.container)
                                    .clickable(role = Role.Checkbox) {
                                        viewModel.edit { f ->
                                            val current = if (f.scheduleType == ScheduleType.EVERY_DAY) DayOfWeek.entries.toSet() else f.days
                                            val next = if (day in current) current - day else current + day
                                            f.copy(days = next, scheduleType = if (next.size == 7) ScheduleType.EVERY_DAY else ScheduleType.SPECIFIC_DAYS)
                                        }
                                    }
                                    .semantics { selected = on; contentDescription = full },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (on) c.highlight else sand.content,
                                )
                            }
                        }
                    }
                }

                // Reminder
                Spacer(Modifier.height(12.dp))
                val timeFormat = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
                BentoTile(sky.container, Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            BentoKicker(stringResource(R.string.bprofile_reminder), sky.content)
                            val first = form.reminders.firstOrNull()
                            Text(
                                first?.format(timeFormat) ?: stringResource(R.string.habit_add_time),
                                style = bigStyle(22.sp), color = sky.content, maxLines = 1,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) {
                                    replaceFirst = first != null
                                    showTimePicker = true
                                },
                            )
                        }
                        Switch(
                            checked = form.remindersEnabled,
                            onCheckedChange = { on ->
                                viewModel.edit { it.copy(remindersEnabled = on) }
                                if (on) askNotifications()
                                if (on && form.reminders.isEmpty()) {
                                    replaceFirst = false
                                    showTimePicker = true
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = c.highlight,
                                checkedTrackColor = c.tileInk.container,
                                checkedBorderColor = c.tileInk.container,
                                uncheckedThumbColor = c.tileInk.container.copy(alpha = 0.55f),
                                uncheckedTrackColor = c.tileInk.content.copy(alpha = 0.45f),
                                uncheckedBorderColor = c.tileInk.container.copy(alpha = 0.55f),
                            ),
                        )
                    }
                    if (form.remindersEnabled) {
                        com.rave.app.ui.permissions.ReminderHealth(Modifier.padding(top = 8.dp))
                        Spacer(Modifier.height(10.dp))
                        val remove = stringResource(R.string.action_remove)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            form.reminders.drop(1).forEach { t ->
                                val label = t.format(timeFormat)
                                BentoChip(
                                    label, c.tileInk.content, c.tileInk.container, icon = Icons.Rounded.Close,
                                    modifier = Modifier.semantics { contentDescription = "$remove $label" },
                                    onClick = { viewModel.edit { it.copy(reminders = it.reminders - t) } },
                                )
                            }
                            if (form.reminders.isNotEmpty()) {
                                BentoChip(
                                    stringResource(R.string.habit_add_time), c.tileInk.container, c.tileInk.content, icon = Icons.Rounded.Add,
                                    onClick = { replaceFirst = false; showTimePicker = true },
                                )
                            }
                        }
                    }
                }

                // Shared streak
                Spacer(Modifier.height(12.dp))
                BentoOutlinedCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.People, null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.habit_share_title), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
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
                        if (!state.signedIn) {
                            Spacer(Modifier.height(10.dp))
                            BentoChip(stringResource(R.string.action_sign_in), c.tileInk.container, c.tileInk.content,
                                modifier = Modifier.height(40.dp), onClick = onSignIn)
                        } else if (state.friends.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            var open by remember { mutableStateOf(false) }
                            Box {
                                BentoChip(form.shareWith?.name ?: stringResource(R.string.habit_share_pick), c.tileInk.container, c.tileInk.content,
                                    modifier = Modifier.height(40.dp), onClick = { open = true })
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
                    Text(stringResource(R.string.habit_goal_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp, start = 4.dp))
                }
                Spacer(Modifier.height(12.dp))
                BentoButton(
                    text = stringResource(if (state.isEdit) R.string.action_save else R.string.bprofile_create_habit),
                    container = c.tileInk.container, content = c.tileInk.content,
                    onClick = viewModel::save, enabled = !state.saving, height = 58.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.isEdit) {
                    TextButton(onClick = { showArchive = true }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)) {
                        Text(stringResource(R.string.habit_archive), color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
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
        val existing = if (replaceFirst) state.form.reminders.firstOrNull() else null
        ReminderTimeDialog(
            initial = existing ?: LocalTime.of(8, 0),
            onDismiss = { showTimePicker = false },
            onRemove = existing?.let { t ->
                {
                    showTimePicker = false
                    viewModel.edit { it.copy(reminders = it.reminders - t) }
                }
            },
            onConfirm = { t ->
                showTimePicker = false
                viewModel.edit {
                    val base = if (existing != null) it.reminders - existing else it.reminders
                    it.copy(reminders = (base + t).distinct().sorted(), remindersEnabled = true)
                }
            },
        )
    }
    if (showIcons) {
        IconPickerDialog(
            selected = state.form.icon,
            tile = state.form.color.tile().container,
            onPick = { key -> viewModel.edit { it.copy(icon = key) }; showIcons = false },
            onDismiss = { showIcons = false },
        )
    }
}

@Composable
private fun SectionKicker(text: String, top: androidx.compose.ui.unit.Dp = 16.dp) {
    BentoKicker(text, MaterialTheme.colorScheme.onBackground, Modifier.padding(top = top, bottom = 8.dp))
}

@Composable
private fun HelpText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), modifier = modifier)
}

/** − (white) and + (black) rounded-square stepper buttons. */
@Composable
private fun StepperButtons(onMinus: () -> Unit, onPlus: () -> Unit) {
    val ink = RaveTheme.colors.tileInk
    BentoIconButton(Icons.Rounded.Remove, stringResource(R.string.bprofile_decrease), onMinus, container = ink.content, content = ink.container, size = 44.dp)
    Spacer(Modifier.width(8.dp))
    BentoIconButton(Icons.Rounded.Add, stringResource(R.string.bprofile_increase), onPlus, container = ink.container, content = ink.content, size = 44.dp)
}

/** Sand tile: kicker, big editable value + suffix ("10 min"), and the stepper buttons. */
@Composable
private fun StepperTile(
    kicker: String,
    value: String,
    suffix: String,
    error: Boolean,
    onValueChange: (String) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    decimal: Boolean = false,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val sand = RaveTheme.colors.tileSand
    val color = if (error) MaterialTheme.colorScheme.error else sand.content
    val style = bigStyle(28.sp).copy(color = color)
    BentoTile(sand.container, Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BentoKicker(kicker, sand.content)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = value,
                        onValueChange = { v -> onValueChange(v.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }.take(6)) },
                        singleLine = true,
                        textStyle = style,
                        cursorBrush = SolidColor(color),
                        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
                        modifier = Modifier.width(IntrinsicSize.Min).semantics { contentDescription = kicker },
                    )
                    if (suffix.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(suffix, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            StepperButtons(onMinus, onPlus)
        }
        extra()
    }
}

/** Small borderless text field on a sand tile (counter unit). */
@Composable
private fun InlineField(value: String, hint: String, onValueChange: (String) -> Unit) {
    val sand = RaveTheme.colors.tileSand
    val style = MaterialTheme.typography.bodyMedium.copy(color = sand.content, fontWeight = FontWeight.SemiBold)
    Box(
        Modifier.fillMaxWidth().clip(SmoothCornerShape(14.dp)).background(sand.content.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (value.isEmpty()) Text(hint, style = style, color = sand.content.copy(alpha = 0.45f), maxLines = 1)
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = true, textStyle = style,
            cursorBrush = SolidColor(sand.content), modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPickerDialog(selected: String, tile: androidx.compose.ui.graphics.Color, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val c = RaveTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
        title = { Text(stringResource(R.string.habit_icon)) },
        text = {
            FlowRow(
                Modifier.verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HabitIcons.all.forEach { (key, icon) ->
                    val on = key == selected
                    Box(
                        Modifier.size(52.dp).clip(SmoothCornerShape(16.dp))
                            .background(if (on) c.tileInk.container else c.tileSand.container)
                            .clickable(role = Role.RadioButton) { onPick(key) }
                            .semantics { this.selected = on; contentDescription = key },
                        contentAlignment = Alignment.Center,
                    ) { Icon(icon, null, tint = if (on) tile else c.tileSand.content, modifier = Modifier.size(26.dp)) }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(initial: LocalTime, onDismiss: () -> Unit, onRemove: (() -> Unit)?, onConfirm: (LocalTime) -> Unit) {
    val pickerState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) { Text(stringResource(R.string.action_ok)) } },
        dismissButton = {
            Row {
                if (onRemove != null) {
                    TextButton(onClick = onRemove) { Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
        title = { Text(stringResource(R.string.habit_add_time)) },
        text = { TimePicker(state = pickerState) },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
