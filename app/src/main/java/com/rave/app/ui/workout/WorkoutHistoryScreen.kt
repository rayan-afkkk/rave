package com.rave.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.rave.app.core.TimeProvider
import com.rave.app.data.local.entity.WorkoutEntity
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.WorkoutRepository
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.util.Format
import com.rave.app.domain.workout.Exercise
import com.rave.app.domain.workout.ExerciseBests
import com.rave.app.domain.workout.ExerciseKind
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.IconTile
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.SectionHeader
import com.rave.app.ui.theme.RaveTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

data class WorkoutHistoryState(
    val loading: Boolean = true,
    val workouts: List<WorkoutEntity> = emptyList(),
    val records: List<Pair<Exercise, ExerciseBests>> = emptyList(),
    val weekCount: Int = 0,
    val weekSets: Int = 0,
    val weekVolumeKg: Double = 0.0,
    val pounds: Boolean = false,
    val hasActive: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    repo: WorkoutRepository,
    settings: SettingsRepository,
    time: TimeProvider,
) : ViewModel() {
    val state: StateFlow<WorkoutHistoryState> = combine(
        repo.observeHistory().mapLatest { it to repo.personalRecords() }, settings.settings, repo.observeActiveWorkout(),
    ) { (history, records), prefs, active ->
        val weekStart = time.startOfDayMs(time.weekStart())
        val week = history.filter { it.startTime >= weekStart }
        WorkoutHistoryState(
            loading = false, workouts = history, records = records.take(8),
            weekCount = week.size, weekSets = week.sumOf { it.workingSets }, weekVolumeKg = week.sumOf { it.totalVolumeKg },
            pounds = prefs.unit == DistanceUnit.MILES, hasActive = active != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutHistoryState())
}

@Composable
fun WorkoutHistoryScreen(onBack: () -> Unit, onStart: () -> Unit, onOpen: (Long) -> Unit, viewModel: WorkoutHistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RaveScreen(title = stringResource(R.string.workouts_title), onBack = onBack) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                PillButton(
                    stringResource(if (state.hasActive) R.string.workout_resume else R.string.workout_start),
                    onClick = onStart, icon = Icons.Rounded.PlayArrow, modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                val c = RaveTheme.colors
                Column(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(c.accentCard).padding(20.dp)) {
                    Text(stringResource(R.string.routes_this_week), style = MaterialTheme.typography.labelLarge, color = c.onAccentCard.copy(alpha = 0.8f))
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        WeekStat(state.weekCount.toString(), stringResource(R.string.workouts_count))
                        WeekStat(state.weekSets.toString(), stringResource(R.string.workout_sets))
                        WeekStat("${formatWeight(state.weekVolumeKg, state.pounds, 0)} ${weightUnit(state.pounds)}", stringResource(R.string.workout_volume))
                    }
                }
            }
            if (state.records.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.workout_records)) }
                item {
                    SectionCard {
                        state.records.forEach { (ex, b) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(ex.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Text(recordLabel(ex.kind, b, state.pounds), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.workouts_history)) }
            if (state.workouts.isEmpty() && !state.loading) {
                item {
                    EmptyState(Icons.Rounded.FitnessCenter, stringResource(R.string.workouts_empty_title), stringResource(R.string.workouts_empty_body),
                        tile = RaveTheme.colors.tilePeach)
                }
            }
            items(state.workouts, key = { it.id }) { w ->
                SectionCard(onClick = { onOpen(w.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Rounded.FitnessCenter, RaveTheme.colors.tilePeach)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(w.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                Instant.ofEpochMilli(w.startTime).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(R.string.workout_row_stats, w.workingSets, "${formatWeight(w.totalVolumeKg, state.pounds, 0)} ${weightUnit(state.pounds)}",
                                    Format.duration(((w.endTime ?: w.startTime) - w.startTime) / 1000)),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (w.prCount > 0) PrTag(stringResource(R.string.workout_pr_count, w.prCount))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekStat(value: String, label: String) {
    val c = RaveTheme.colors
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = c.onAccentCard)
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.onAccentCard.copy(alpha = 0.75f))
    }
}

@Composable
private fun recordLabel(kind: ExerciseKind, b: ExerciseBests, pounds: Boolean): String = when (kind) {
    ExerciseKind.WEIGHT_REPS -> stringResource(R.string.workout_record_weight, formatWeight(b.heaviestKg, pounds, 1), weightUnit(pounds),
        formatWeight(b.best1rmKg, pounds, 0))
    ExerciseKind.BODYWEIGHT_REPS -> stringResource(R.string.workout_record_reps, b.mostReps)
    ExerciseKind.DURATION -> Format.duration(b.longestHoldS.toLong())
}
