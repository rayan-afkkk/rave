package com.rave.app.ui.home

import android.Manifest
import com.rave.app.ui.components.SmoothCornerShape
import com.rave.app.ui.components.pressable
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rave.app.R
import com.rave.app.domain.util.Format
import com.rave.app.ui.settings.isIgnoringBatteryOptimizations
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.unitLabel
import java.text.NumberFormat
import java.time.format.TextStyle
import java.util.Locale

/*
 * Bento Pop Home: bold colour tiles with big numbers.
 * Steps (black, with the week's bars) · Streak (lime) · Water (sky) · Habits (lilac) · Start a walk (orange).
 */

private val TileShape = SmoothCornerShape(26.dp)
private val TilePad = 16.dp
private const val STEPS_TILE_HEIGHT = 220

private fun fmt(n: Int) = NumberFormat.getIntegerInstance().format(n)

@Composable
private fun Label(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
        color = color.copy(alpha = 0.65f), modifier = modifier)
}

@Composable
private fun Big(text: String, color: Color, size: TextUnit = 32.sp) {
    Text(text, color = color, style = MaterialTheme.typography.displaySmall.copy(fontSize = size, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp, lineHeight = size))
}

@Composable
private fun Tile(color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        modifier.then(if (onClick != null) Modifier.pressable(TileShape, pressedScale = 0.97f, onClick = onClick) else Modifier)
            .clip(TileShape).background(color).padding(TilePad),
    ) { content() }
}

/** "Let's go, Rayan ⚡" with an initial tile. */
@Composable
fun BentoHeader(state: HomeUiState, modifier: Modifier = Modifier) {
    val name = state.name.ifBlank { stringResource(R.string.guest_name) }.substringBefore(' ')
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.bento_greeting, name),
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, lineHeight = 30.sp),
            color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f),
        )
        Box(
            Modifier.size(48.dp).clip(SmoothCornerShape(14.dp)).background(RaveTheme.colors.tilePeach.container),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.take(1).uppercase(Locale.getDefault()), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = RaveTheme.colors.tilePeach.content)
        }
    }
}

/** The bento grid at the top of Home. */
@Composable
fun BentoGrid(
    state: HomeUiState,
    onToggleHabit: (HabitItem) -> Unit,
    onAddHabit: () -> Unit,
    onStartWalk: () -> Unit,
    onOpenBatteryGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().height(STEPS_TILE_HEIGHT.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StepsTile(onOpenBatteryGuide, Modifier.weight(1.25f).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StreakTile(state.overallStreak, state.freezes, Modifier.weight(1f).fillMaxWidth())
                WaterTile(Modifier.weight(1f).fillMaxWidth())
            }
        }
        HabitsTile(state, onToggleHabit, onAddHabit)
        StartWalkTile(onStartWalk)
    }
}

@Composable
private fun StepsTile(onOpenBatteryGuide: () -> Unit, modifier: Modifier, viewModel: TodayActivityViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val restricted = remember { !isIgnoringBatteryOptimizations(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refreshPermission() }
    LaunchedEffect(s.permitted, s.sensorAvailable) { if (s.permitted && s.sensorAvailable) viewModel.countLive() }
    val ink = RaveTheme.colors.accentCard
    val on = RaveTheme.colors.onAccentCard
    val lime = RaveTheme.colors.highlight
    Tile(ink, modifier, onClick = if (!s.permitted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        { launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
    } else null) {
        Column(Modifier.fillMaxSize()) {
            Label(stringResource(R.string.bento_steps), on)
            when {
                !s.sensorAvailable -> Text(stringResource(R.string.fitness_no_sensor), color = on, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 10.dp))
                !s.permitted -> Text(stringResource(R.string.fitness_permission_body), color = on, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 10.dp))
                else -> {
                    Spacer(Modifier.height(10.dp))
                    Big(fmt(s.steps), on, 36.sp)
                    Text(
                        stringResource(R.string.bento_steps_sub, fmt(s.goal), Format.distance(s.distanceM, s.unit), unitLabel(s.unit)),
                        style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.weight(1f))
                    if (s.week.isNotEmpty()) WeekBars(s.week.map { it.first to it.second }, s.goal, lime, on)
                    if (restricted) {
                        Text(stringResource(R.string.fitness_battery_hint), style = MaterialTheme.typography.labelSmall, color = lime,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp).clickable(onClick = onOpenBatteryGuide))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekBars(week: List<Pair<java.time.LocalDate, Int>>, goal: Int, lime: Color, on: Color) {
    val max = maxOf(goal, week.maxOf { it.second }).coerceAtLeast(1)
    val today = week.last().first
    Row(Modifier.fillMaxWidth().height(86.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        week.forEach { (date, value) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f).width(12.dp), contentAlignment = Alignment.BottomCenter) {
                    val f = (value / max.toFloat()).coerceIn(0.06f, 1f)
                    Box(Modifier.fillMaxWidth().fillMaxHeight(f).clip(CircleShape)
                        .background(if (date == today || value >= goal) lime else on.copy(alpha = 0.22f)))
                }
                Text(date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall,
                    color = on.copy(alpha = if (date == today) 1f else 0.6f), modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun StreakTile(streak: Int, freezes: Int, modifier: Modifier) {
    val t = RaveTheme.colors.tileGreen
    Tile(t.container, modifier) {
        Column {
            Label(stringResource(R.string.bento_streak), t.content)
            Spacer(Modifier.height(6.dp))
            Big("$streak🔥", t.content, 28.sp)
            Text(stringResource(R.string.bento_streak_sub, freezes), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = t.content, maxLines = 1)
        }
    }
}

@Composable
private fun WaterTile(modifier: Modifier, viewModel: WaterViewModel = hiltViewModel()) {
    val w by viewModel.state.collectAsStateWithLifecycle()
    val t = RaveTheme.colors.tileBlue
    Tile(t.container, modifier, onClick = { viewModel.add(1) }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(stringResource(R.string.water_title), t.content, Modifier.weight(1f))
                Icon(Icons.Rounded.Add, stringResource(R.string.water_add), tint = t.content, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Big("${w.glasses}", t.content, 28.sp)
                Text("/${w.goal}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = t.content,
                    modifier = Modifier.padding(bottom = 4.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                repeat(w.goal.coerceIn(1, 10)) { i ->
                    Icon(Icons.Rounded.WaterDrop, null, tint = if (i < w.glasses) t.content else t.content.copy(alpha = 0.2f), modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
private fun HabitsTile(state: HomeUiState, onToggle: (HabitItem) -> Unit, onAdd: () -> Unit) {
    val t = RaveTheme.colors.tilePink
    Tile(t.container, Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(stringResource(R.string.bento_habits, state.doneCount, state.totalCount), t.content, Modifier.weight(1f))
                Text(
                    stringResource(R.string.bento_add), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = RaveTheme.colors.onAccentCard,
                    modifier = Modifier.clip(CircleShape).background(RaveTheme.colors.accentCard).clickable(onClick = onAdd)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
            if (state.items.isEmpty()) {
                Text(stringResource(R.string.home_empty_message), style = MaterialTheme.typography.bodyMedium, color = t.content,
                    modifier = Modifier.padding(top = 10.dp))
            } else {
                Spacer(Modifier.height(10.dp))
                state.items.take(4).chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        pair.forEach { item -> HabitCheck(item, t.content, state.editable, Modifier.weight(1f)) { onToggle(item) } }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitCheck(item: HabitItem, ink: Color, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.clip(RoundedCornerShape(10.dp)).clickable(enabled = enabled, onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(24.dp).clip(RoundedCornerShape(7.dp))
                .then(if (item.completed) Modifier.background(ink) else Modifier.border(2.dp, ink, RoundedCornerShape(7.dp))),
            contentAlignment = Alignment.Center,
        ) {
            if (item.completed) Icon(Icons.Rounded.Check, null, tint = RaveTheme.colors.tilePink.container, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(item.habit.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StartWalkTile(onClick: () -> Unit, viewModel: TodayActivityViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val cta = RaveTheme.colors.cta
    Tile(cta.container, Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.bento_start_walk), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = cta.content)
                val left = s.goal - s.steps
                Text(
                    if (left > 0) stringResource(R.string.bento_steps_left, fmt(left)) else stringResource(R.string.bento_goal_done),
                    style = MaterialTheme.typography.bodySmall, color = cta.content.copy(alpha = 0.8f),
                )
            }
            Box(Modifier.size(52.dp).clip(CircleShape).background(RaveTheme.colors.accentCard), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = RaveTheme.colors.onAccentCard, modifier = Modifier.size(28.dp))
            }
        }
    }
}

