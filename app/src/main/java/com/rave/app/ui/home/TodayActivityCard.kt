package com.rave.app.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.rave.app.core.TimeProvider
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.ActivityRepository
import com.rave.app.data.repository.StepRepository
import com.rave.app.data.repository.WorkoutRepository
import com.rave.app.domain.fitness.FitnessMath
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.util.Format
import com.rave.app.tracking.steps.StepSensor
import com.rave.app.ui.components.IconTile
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.TonalPillButton
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.theme.TileColor
import com.rave.app.ui.unitLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class TodayActivityState(
    val sensorAvailable: Boolean = true,
    val permitted: Boolean = true,
    val steps: Int = 0,
    val goal: Int = FitnessMath.DEFAULT_STEP_GOAL,
    val distanceM: Double = 0.0,
    val calories: Int = 0,
    val activeMinutes: Int = 0,
    val week: List<Pair<LocalDate, Int>> = emptyList(),
    val unit: DistanceUnit = DistanceUnit.KM,
) {
    val progress: Float get() = if (goal <= 0) 0f else (steps / goal.toFloat()).coerceIn(0f, 1f)
}

/**
 * Everyday fitness for Home: steps from the phone's step counter plus distance, calories and
 * active minutes. Calories are the larger of the step estimate and tracked walks/runs, so a
 * tracked run isn't counted twice; active minutes come from tracked activities and workouts.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayActivityViewModel @Inject constructor(
    private val sensor: StepSensor,
    private val steps: StepRepository,
    activities: ActivityRepository,
    workouts: WorkoutRepository,
    settings: SettingsRepository,
    time: TimeProvider,
) : ViewModel() {
    private val permitted = MutableStateFlow(sensor.hasPermission())

    private data class Tracked(val kcal: Int, val activeS: Long)

    private val trackedToday = time.todayFlow.flatMapLatest { today ->
        combine(activities.observeBetween(today, today), workouts.observeHistory()) { acts, history ->
            val workoutS = history.filter { it.endTime != null && time.dateOf(it.startTime) == today }
                .sumOf { ((it.endTime ?: it.startTime) - it.startTime) / 1000 }
            Tracked(acts.sumOf { it.calories }, acts.sumOf { it.movingTimeS.takeIf { m -> m > 0 } ?: it.durationS } + workoutS)
        }
    }

    val state: StateFlow<TodayActivityState> = combine(
        steps.observeToday(), steps.observeLastDays(7), settings.settings, trackedToday, permitted,
    ) { today, week, prefs, tracked, ok ->
        TodayActivityState(
            sensorAvailable = sensor.isAvailable,
            permitted = ok,
            steps = today,
            goal = prefs.dailyStepGoal,
            distanceM = FitnessMath.distanceM(today, prefs.heightCm),
            calories = maxOf(FitnessMath.stepCalories(today, prefs.weightKg, prefs.heightCm), tracked.kcal),
            activeMinutes = (tracked.activeS / 60).toInt(),
            week = week,
            unit = prefs.unit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayActivityState())

    fun refreshPermission() { permitted.value = sensor.hasPermission() }

    /** Live counting while the card is on screen. */
    suspend fun countLive() {
        if (!sensor.canCount) return
        sensor.counter().collect { steps.record(it) }
    }
}

@Composable
fun TodayActivityCard(modifier: Modifier = Modifier, viewModel: TodayActivityViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refreshPermission() }
    LaunchedEffect(state.permitted, state.sensorAvailable) {
        if (state.permitted && state.sensorAvailable) viewModel.countLive()
    }

    SectionCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.fitness_today_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (state.sensorAvailable && state.permitted) {
                Text(
                    stringResource(R.string.fitness_goal, formatInt(state.goal)),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        when {
            !state.sensorAvailable -> Text(
                stringResource(R.string.fitness_no_sensor), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            !state.permitted -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.fitness_permission_body), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TonalPillButton(
                    stringResource(R.string.fitness_permission_action),
                    onClick = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                    icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
                )
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepRing(state.steps, state.progress)
                    Spacer(Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                        val colors = RaveTheme.colors
                        FitnessStat(Icons.AutoMirrored.Rounded.DirectionsWalk, colors.tileGreen,
                            "${Format.distance(state.distanceM, state.unit)} ${unitLabel(state.unit)}", stringResource(R.string.fitness_distance))
                        FitnessStat(Icons.Rounded.LocalFireDepartment, colors.tilePeach,
                            stringResource(R.string.fitness_kcal, formatInt(state.calories)), stringResource(R.string.fitness_calories))
                        FitnessStat(Icons.Rounded.Timer, colors.tileBlue,
                            stringResource(R.string.fitness_minutes, state.activeMinutes), stringResource(R.string.fitness_active))
                    }
                }
                if (state.week.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    WeekSteps(state.week, state.goal)
                }
            }
        }
    }
}

@Composable
private fun StepRing(steps: Int, progress: Float) {
    val animated by animateFloatAsState(progress, tween(900), label = "steps")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(112.dp)) {
            val stroke = 10.dp.toPx()
            // Inset by half the stroke so the ring isn't clipped at the edges.
            val inset = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, topLeft = inset, size = arcSize, style = Stroke(stroke))
            if (animated > 0f) {
                drawArc(accent, -90f, 360f * animated, false, topLeft = inset, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatInt(steps), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(stringResource(R.string.fitness_steps), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FitnessStat(icon: ImageVector, tile: TileColor, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, tile, size = 32.dp, iconSize = 18.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Last 7 days of steps; full colour once the goal was reached, today outlined by weight. */
@Composable
private fun WeekSteps(week: List<Pair<LocalDate, Int>>, goal: Int) {
    val max = maxOf(goal, week.maxOf { it.second }).coerceAtLeast(1)
    val accent = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val today = week.last().first
    Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        week.forEach { (date, value) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(Modifier.weight(1f).width(14.dp).background(track, RoundedCornerShape(7.dp)), contentAlignment = Alignment.BottomCenter) {
                    val fraction = (value / max.toFloat()).coerceIn(0f, 1f)
                    if (fraction > 0f) {
                        Box(
                            Modifier.fillMaxWidth().fillMaxHeight(fraction.coerceAtLeast(0.08f))
                                .background(if (value >= goal) accent else soft, RoundedCornerShape(7.dp)),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal),
                    color = if (date == today) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatInt(value: Int): String = NumberFormat.getIntegerInstance().format(value)
