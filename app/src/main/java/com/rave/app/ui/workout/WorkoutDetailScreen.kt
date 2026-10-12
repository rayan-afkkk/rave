package com.rave.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rave.app.R
import com.rave.app.core.TimeProvider
import com.rave.app.data.local.entity.WorkoutEntity
import com.rave.app.data.local.entity.WorkoutSetEntity
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.HabitRepository
import com.rave.app.data.repository.WorkoutRepository
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.model.LogSource
import com.rave.app.domain.util.Format
import com.rave.app.domain.workout.Exercise
import com.rave.app.domain.workout.SetPr
import com.rave.app.ui.components.ConfirmDialog
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.navigation.WorkoutDetailRoute
import com.rave.app.ui.theme.PillShape
import com.rave.app.ui.theme.RaveTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

data class WorkoutDetailState(
    val loading: Boolean = true,
    val workout: WorkoutEntity? = null,
    val groups: List<Pair<Exercise, List<WorkoutSetEntity>>> = emptyList(),
    val prs: Map<Long, Set<SetPr>> = emptyMap(),
    val ticked: List<String> = emptyList(),
    val pounds: Boolean = false,
    val closed: Boolean = false,
)

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: WorkoutRepository,
    habits: HabitRepository,
    settings: SettingsRepository,
    time: TimeProvider,
) : ViewModel() {
    private val route = savedState.toRoute<WorkoutDetailRoute>()
    val isNew = route.isNew
    private val ticked = MutableStateFlow<List<String>>(emptyList())
    private val closed = MutableStateFlow(false)

    val state: StateFlow<WorkoutDetailState> = combine(
        repo.observeWorkout(route.workoutId), repo.observeSets(route.workoutId), repo.observeExercises(),
        combine(settings.settings, ticked) { a, b -> a to b }, closed,
    ) { w, sets, library, (prefs, t), c ->
        val byId = library.associateBy { it.id }
        WorkoutDetailState(
            loading = false, workout = w,
            groups = sets.groupBy { it.exerciseId }.entries.sortedBy { (_, s) -> s.minOf { it.exerciseOrder } }
                .mapNotNull { (id, s) -> byId[id]?.let { it to s.sortedBy { x -> x.setOrder } } },
            prs = w?.let { repo.parsePrTags(it.prTags) }.orEmpty(),
            ticked = t, pounds = prefs.unit == DistanceUnit.MILES, closed = c,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutDetailState())

    init {
        viewModelScope.launch {
            val w = repo.observeWorkout(route.workoutId).firstOrNull() ?: return@launch
            val date = time.dateOf(w.startTime)
            val logs = habits.logsFor(date)
            ticked.value = habits.activeHabits().filter { logs[it.id]?.source == LogSource.WORKOUT && logs[it.id]?.completed == true }.map { it.name }
        }
    }

    fun save(name: String, note: String) {
        viewModelScope.launch {
            repo.rename(route.workoutId, name, note)
            closed.value = true
        }
    }

    fun delete() {
        viewModelScope.launch {
            repo.delete(route.workoutId)
            closed.value = true
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutDetailScreen(onClose: () -> Unit, viewModel: WorkoutDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.closed) { if (state.closed) onClose() }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val w = state.workout
    var name by rememberSaveable(w?.id) { mutableStateOf(w?.name.orEmpty()) }
    var note by rememberSaveable(w?.id) { mutableStateOf(w?.note.orEmpty()) }

    RaveScreen(
        title = stringResource(if (viewModel.isNew) R.string.workout_done_title else R.string.workout_title),
        onBack = { if (viewModel.isNew) viewModel.save(name, note) else onClose() },
    ) {
        if (state.loading || w == null) {
            LoadingState()
            return@RaveScreen
        }
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                val c = RaveTheme.colors
                Column(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(c.accentCard).padding(20.dp)) {
                    Text(
                        Instant.ofEpochMilli(w.startTime).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)),
                        style = MaterialTheme.typography.labelMedium, color = c.onAccentCard.copy(alpha = 0.8f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Big(Format.duration(((w.endTime ?: w.startTime) - w.startTime) / 1000), stringResource(R.string.stat_time))
                        Big(w.workingSets.toString(), stringResource(R.string.workout_sets))
                        Big("${formatWeight(w.totalVolumeKg, state.pounds, 0)} ${weightUnit(state.pounds)}", stringResource(R.string.workout_volume))
                        Big(w.prCount.toString(), stringResource(R.string.workout_prs))
                    }
                }
            }
            if (state.ticked.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(RaveTheme.colors.tileGreen.container).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = RaveTheme.colors.tileGreen.content)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.summary_ticked, state.ticked.joinToString(", ")), style = MaterialTheme.typography.titleSmall,
                            color = RaveTheme.colors.tileGreen.content)
                    }
                }
            }
            if (viewModel.isNew) {
                item {
                    OutlinedTextField(name, { name = it.take(60) }, label = { Text(stringResource(R.string.summary_name)) }, singleLine = true,
                        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
                }
            }
            items(state.groups, key = { it.first.id }) { (ex, sets) ->
                SectionCard {
                    Text(ex.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    state.prs[ex.id]?.takeIf { it.isNotEmpty() }?.let { tags ->
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            tags.forEach { PrTag(stringResource(prRes(it))) }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    var n = 0
                    sets.forEach { s ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(if (s.warmup) "W" else (++n).toString(), style = MaterialTheme.typography.labelLarge,
                                color = if (s.warmup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(32.dp))
                            Text(previousLabel(s, ex.kind, state.pounds), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            item {
                if (viewModel.isNew) {
                    OutlinedTextField(note, { note = it.take(300) }, label = { Text(stringResource(R.string.summary_note)) }, minLines = 2,
                        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    PillButton(stringResource(R.string.action_save), onClick = { viewModel.save(name, note) }, modifier = Modifier.fillMaxWidth())
                } else if (w.note.isNotBlank()) {
                    Text(w.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.workout_delete_title),
            message = stringResource(R.string.workout_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { confirmDelete = false; viewModel.delete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Big(value: String, label: String) {
    val c = RaveTheme.colors
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, color = c.onAccentCard)
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.onAccentCard.copy(alpha = 0.75f))
    }
}

@Composable
fun PrTag(label: String) {
    Surface(shape = PillShape, color = RaveTheme.colors.tilePeach.container) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.EmojiEvents, null, tint = RaveTheme.colors.tilePeach.content, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = RaveTheme.colors.tilePeach.content)
        }
    }
}

fun prRes(pr: SetPr): Int = when (pr) {
    SetPr.HEAVIEST -> R.string.pr_heaviest
    SetPr.BEST_1RM -> R.string.pr_1rm
    SetPr.MOST_REPS -> R.string.pr_reps
    SetPr.BEST_VOLUME -> R.string.pr_volume
    SetPr.LONGEST_HOLD -> R.string.pr_hold
}
