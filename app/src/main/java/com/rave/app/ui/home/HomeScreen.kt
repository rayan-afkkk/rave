package com.rave.app.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rave.app.R
import com.rave.app.domain.model.ActivityGoalUnit
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.HabitType
import com.rave.app.domain.model.LogSource
import com.rave.app.domain.model.ScheduleType
import com.rave.app.domain.util.DayPeriod
import com.rave.app.domain.util.Format
import com.rave.app.domain.util.dayPeriodFor
import com.rave.app.ui.components.AnimatedCheck
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.HabitIcons
import com.rave.app.ui.components.IconTile
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.PillProgress
import com.rave.app.ui.components.ProgressRing
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.SectionHeader
import com.rave.app.ui.components.StreakChip
import com.rave.app.ui.components.TonalPillButton
import com.rave.app.ui.components.tile
import com.rave.app.ui.theme.Dimens
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.unitLabel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onStartActivity: (ActivityType) -> Unit,
    onAddHabit: () -> Unit,
    onEditHabit: (Long) -> Unit,
    onOpenTimer: (Long) -> Unit,
    onOpenProgress: () -> Unit,
    onOpenBatteryGuide: () -> Unit,
    onOpenWrapped: (Int) -> Unit,
    onOpenFood: () -> Unit,
    onScanFood: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) {
        LoadingState()
        return
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 72.dp,
                start = Dimens.ScreenPadding, end = Dimens.ScreenPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
        ) {
            item(key = "header") { Header(state) }
            item(key = "repair") { StreakRepairCard() }
            com.rave.app.domain.wrapped.WrappedCalculator.seasonYear(java.time.LocalDate.now())?.let { year ->
                item(key = "wrapped") { WrappedBanner(year) { onOpenWrapped(year) } }
            }
            item(key = "progress") { ProgressCard(state, onOpenProgress) }
            item(key = "quick") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton(
                        stringResource(R.string.start_walk), onClick = { onStartActivity(ActivityType.WALK) },
                        icon = Icons.AutoMirrored.Rounded.DirectionsWalk, modifier = Modifier.weight(1f),
                    )
                    TonalPillButton(
                        stringResource(R.string.start_run), onClick = { onStartActivity(ActivityType.RUN) },
                        icon = Icons.AutoMirrored.Rounded.DirectionsRun, modifier = Modifier.weight(1f),
                    )
                }
            }
            item(key = "fitness") { TodayActivityCard(onOpenBatteryGuide) }
            item(key = "food") { FoodTodayCard(onOpenFood, onScanFood) }
            item(key = "buddy") { BuddyCard() }
            item(key = "league") { LeagueCard() }
            item(key = "week") { WeekStrip(state, viewModel::selectDate) }
            item(key = "routine_header") {
                val title = if (state.selectedDate == state.today) stringResource(R.string.home_routine_today)
                else stringResource(R.string.home_routine_on, state.selectedDate.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())))
                SectionHeader(title) {
                    if (!state.editable) {
                        Icon(Icons.Rounded.Lock, contentDescription = stringResource(R.string.home_read_only),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (state.items.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.Spa,
                        title = stringResource(R.string.home_empty_title),
                        message = stringResource(R.string.home_empty_message),
                        action = if (state.selectedDate == state.today) {
                            { PillButton(stringResource(R.string.home_add_first), onClick = onAddHabit, icon = Icons.Rounded.Add) }
                        } else null,
                    )
                }
            } else {
                items(state.items, key = { it.habit.id }) { item ->
                    SwipeableHabitCard(
                        item = item,
                        editable = state.editable,
                        unit = state.unit,
                        onToggle = { viewModel.toggle(item) },
                        onComplete = { viewModel.complete(item) },
                        onSkip = { viewModel.skip(item) },
                        onIncrement = { viewModel.increment(item) },
                        onTimer = { onOpenTimer(item.habit.id) },
                        onEdit = { onEditHabit(item.habit.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddHabit,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = contentPadding.calculateBottomPadding()),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.home_add_habit))
        }
    }
}

@Composable
private fun Header(state: HomeUiState) {
    val hour = LocalTime.now().hour
    val greetingRes = when (dayPeriodFor(hour)) {
        DayPeriod.MORNING -> R.string.greeting_morning
        DayPeriod.AFTERNOON -> R.string.greeting_afternoon
        DayPeriod.EVENING, DayPeriod.NIGHT -> R.string.greeting_evening
    }
    val name = state.name.ifBlank { stringResource(R.string.guest_name) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(greetingRes, name.substringBefore(' ')),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                state.today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault())),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.freezes > 0) {
                Icon(Icons.Rounded.AcUnit, contentDescription = pluralStringResource(R.plurals.freezes_count, state.freezes, state.freezes),
                    tint = RaveTheme.colors.tileBlue.content, modifier = Modifier.size(20.dp))
                Text(" ${state.freezes}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.width(8.dp))
            }
            StreakChip(days = state.overallStreak)
        }
    }
}

@Composable
private fun ProgressCard(state: HomeUiState, onClick: () -> Unit) {
    val colors = RaveTheme.colors
    val progress = if (state.totalCount == 0) 0f else state.doneCount / state.totalCount.toFloat()
    val left = state.totalCount - state.doneCount
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(colors.accentCard)
            .clickable(onClickLabel = stringResource(R.string.home_open_progress), onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = progress,
                size = 88.dp,
                strokeWidth = 9.dp,
                trackColor = colors.onAccentCard.copy(alpha = 0.15f),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.doneCount}/${state.totalCount}", style = MaterialTheme.typography.headlineSmall, color = colors.onAccentCard)
                    Text(stringResource(R.string.home_ring_label), style = MaterialTheme.typography.labelSmall, color = colors.onAccentCard.copy(alpha = 0.75f))
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        state.totalCount == 0 -> stringResource(R.string.home_ring_none)
                        left == 0 -> stringResource(R.string.home_ring_all_done)
                        else -> pluralStringResource(R.plurals.home_left, left, left)
                    },
                    style = MaterialTheme.typography.titleMedium, color = colors.onAccentCard,
                )
                if (left > 0 && state.nextHabit != null) {
                    Text(stringResource(R.string.home_next, state.nextHabit), style = MaterialTheme.typography.bodyMedium,
                        color = colors.onAccentCard.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(14.dp))
                val unit = unitLabel(state.unit)
                val kmToday = Format.distance(state.kmToday * 1000, state.unit, 1)
                val goal = Format.distance(state.kmGoal * 1000, state.unit, 1)
                Text(stringResource(R.string.home_distance_vs_goal, kmToday, goal, unit), style = MaterialTheme.typography.labelMedium,
                    color = colors.onAccentCard)
                Spacer(Modifier.height(6.dp))
                PillProgress(
                    progress = if (state.kmGoal <= 0) 0f else (state.kmToday / state.kmGoal).toFloat(),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun WeekStrip(state: HomeUiState, onSelect: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        state.week.forEach { day ->
            val isToday = day == state.today
            val isSelected = day == state.selectedDate
            val future = day.isAfter(state.today)
            val label = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val description = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        when {
                            isToday -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surface
                        },
                    )
                    .then(if (isSelected && !isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.medium) else Modifier)
                    .clickable(enabled = !future, role = Role.Tab) { onSelect(day) }
                    .semantics { selected = isSelected; contentDescription = description }
                    .heightIn(min = 54.dp)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val textColor = when {
                    isToday -> MaterialTheme.colorScheme.onPrimary
                    future -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Text(label.take(3), style = MaterialTheme.typography.labelSmall, color = textColor)
                Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = textColor)
                val p = state.weekProgress[day] ?: 0f
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                p >= 1f -> if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary
                                p > 0f -> if (isToday) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.background.copy(alpha = 0f)
                            },
                        ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableHabitCard(
    item: HabitItem,
    editable: Boolean,
    unit: com.rave.app.domain.model.DistanceUnit,
    onToggle: () -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onIncrement: () -> Unit,
    onTimer: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onComplete()
                SwipeToDismissBoxValue.EndToStart -> onSkip()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false // always snap back; the list re-sorts instead
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = editable && !item.completed,
        enableDismissFromEndToStart = editable,
        modifier = modifier,
        backgroundContent = {
            val toRight = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Row(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (toRight) RaveTheme.colors.tileGreen.container else RaveTheme.colors.tileBlue.container)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = if (toRight) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (toRight) {
                    Icon(Icons.Rounded.Check, null, tint = RaveTheme.colors.tileGreen.content)
                    Text(" " + stringResource(R.string.action_complete), color = RaveTheme.colors.tileGreen.content, style = MaterialTheme.typography.labelLarge)
                } else {
                    Text(stringResource(if (item.skipped) R.string.action_unskip else R.string.action_skip) + " ",
                        color = RaveTheme.colors.tileBlue.content, style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.AutoMirrored.Rounded.Redo, null, tint = RaveTheme.colors.tileBlue.content)
                }
            }
        },
    ) {
        HabitCard(item, editable, unit, onToggle, onIncrement, onTimer, onEdit)
    }
}

@Composable
private fun HabitCard(
    item: HabitItem,
    editable: Boolean,
    unit: com.rave.app.domain.model.DistanceUnit,
    onToggle: () -> Unit,
    onIncrement: () -> Unit,
    onTimer: () -> Unit,
    onEdit: () -> Unit,
) {
    val habit = item.habit
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClickLabel = stringResource(R.string.action_edit), onClick = onEdit)
            .heightIn(min = Dimens.RowMinHeight)
            .padding(start = 2.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedCheck(
            checked = item.completed,
            skipped = item.skipped,
            enabled = editable,
            onCheckedChange = if (editable) { _ -> onToggle() } else null,
            contentDescription = stringResource(if (item.completed) R.string.cd_mark_not_done else R.string.cd_mark_done, habit.name),
        )
        IconTile(HabitIcons.get(habit.icon), habit.color.tile())
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                habit.name,
                style = MaterialTheme.typography.titleSmall,
                color = if (item.skipped) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                habitSubtitle(item, unit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        when {
            habit.type == HabitType.COUNTER && editable && !item.completed -> FilledTonalIconButton(onClick = onIncrement) {
                Text("+1", style = MaterialTheme.typography.labelLarge)
            }
            habit.type == HabitType.TIMED && editable && !item.completed -> FilledTonalIconButton(onClick = onTimer) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.cd_start_timer, habit.name))
            }
            else -> Text(trailingLabel(item, unit), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun trailingLabel(item: HabitItem, unit: com.rave.app.domain.model.DistanceUnit): String {
    val h = item.habit
    return when (h.type) {
        HabitType.TIMED -> stringResource(R.string.minutes_short, h.target)
        HabitType.ACTIVITY -> if (h.activityGoalUnit == ActivityGoalUnit.MINUTES) stringResource(R.string.minutes_short, h.target)
            else "${Format.distance(h.target.toDouble(), unit, 1)} ${unitLabel(unit)}"
        HabitType.COUNTER -> "${h.target} ${h.unit.orEmpty()}".trim()
        HabitType.CHECKBOX -> ""
        HabitType.WORKOUT -> pluralStringResource(R.plurals.habit_workout_target, h.target, h.target)
    }
}

@Composable
private fun habitSubtitle(item: HabitItem, unit: com.rave.app.domain.model.DistanceUnit): String {
    val h = item.habit
    val parts = mutableListOf<String>()
    if (item.skipped) parts += stringResource(R.string.habit_sub_skipped)
    if (h.type == HabitType.ACTIVITY && (item.log?.source == LogSource.GPS || item.log?.source == LogSource.AUTO) && item.value > 0) {
        val progress = if (h.activityGoalUnit == ActivityGoalUnit.MINUTES) stringResource(R.string.minutes_short, item.value)
            else "${Format.distance(item.value.toDouble(), unit, 1)} ${unitLabel(unit)}"
        parts += if (item.completed) stringResource(R.string.habit_sub_auto_gps, progress) else progress
    } else if (h.type == HabitType.WORKOUT && item.value > 0) {
        parts += if (item.completed) stringResource(R.string.habit_sub_auto_workout, item.value)
            else stringResource(R.string.habit_sub_workout_progress, item.value, h.target)
    } else when (h.type) {
        HabitType.COUNTER -> parts += stringResource(R.string.habit_sub_counter, item.value, h.target, h.unit.orEmpty()).trim()
        HabitType.TIMED -> if (item.value > 0 && !item.completed) parts += stringResource(R.string.habit_sub_timed, item.value, h.target)
        else -> Unit
    }
    if (h.schedule.type == ScheduleType.TIMES_PER_WEEK) parts += stringResource(R.string.habit_sub_weekly, item.weekCount, h.schedule.timesPerWeek)
    h.sharedWithName?.let { parts += stringResource(R.string.habit_sub_with_friend, it) }
    if (item.streak > 0) parts += pluralStringResource(R.plurals.habit_streak_days, item.streak, item.streak)
    if (parts.isEmpty() && h.remindersEnabled && h.reminderTimes.isNotEmpty()) {
        val t = h.reminderTimes.first().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        parts += stringResource(R.string.habit_sub_reminder, t)
    }
    if (parts.isEmpty()) parts += stringResource(R.string.habit_sub_tap_to_start)
    return parts.take(2).joinToString(" · ")
}

/** December–January: invites the user to their yearly "Rave Wrapped". */
@Composable
private fun WrappedBanner(year: Int, onClick: () -> Unit) {
    val colors = RaveTheme.colors
    SectionCard(Modifier.fillMaxWidth(), color = colors.splashBackground, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.rave.app.ui.components.RaveLogo(color = colors.splashMark, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.wrapped_banner_title, year), style = MaterialTheme.typography.titleMedium, color = colors.splashText)
                Text(stringResource(R.string.wrapped_banner_body), style = MaterialTheme.typography.bodySmall, color = colors.splashText.copy(alpha = 0.75f))
            }
        }
    }
}
