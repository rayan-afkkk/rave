package com.rave.app.ui.workout

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rave.app.R
import com.rave.app.data.local.entity.WorkoutSetEntity
import com.rave.app.domain.util.Format
import com.rave.app.domain.workout.ExerciseKind
import com.rave.app.ui.components.ConfirmDialog
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.PillProgress
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.theme.PillShape
import com.rave.app.ui.theme.RaveTheme
import java.util.Locale

@Composable
fun WorkoutScreen(onBack: () -> Unit, onFinished: (Long) -> Unit, viewModel: WorkoutViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var picking by rememberSaveable { mutableStateOf(false) }
    var confirmFinish by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val emptyMsg = stringResource(R.string.workout_empty_finish)

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is WorkoutEvent.Finished -> onFinished(e.workoutId)
                WorkoutEvent.Empty -> { Toast.makeText(context, emptyMsg, Toast.LENGTH_SHORT).show(); onBack() }
                WorkoutEvent.Discarded -> onBack()
            }
        }
    }
    // Buzz when the rest is over.
    var wasResting by remember { mutableStateOf(false) }
    LaunchedEffect(state.restLeftS, state.restEndsAt) {
        val resting = state.restEndsAt != null && state.restLeftS > 0
        if (wasResting && !resting) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        wasResting = resting
    }

    RaveScreen(
        title = state.workout?.name ?: stringResource(R.string.workout_title),
        onBack = onBack,
        actions = {
            TextButton(onClick = { confirmFinish = true }, enabled = state.workout != null) {
                Text(stringResource(R.string.action_finish), style = MaterialTheme.typography.labelLarge)
            }
        },
    ) {
        if (state.loading) {
            LoadingState()
            return@RaveScreen
        }
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.imePadding(),
            ) {
                item { SummaryStrip(state) }
                if (state.blocks.isEmpty()) {
                    item {
                        EmptyState(Icons.Rounded.FitnessCenter, stringResource(R.string.workout_empty_title),
                            stringResource(R.string.workout_empty_body), tile = RaveTheme.colors.tilePeach)
                    }
                }
                items(state.blocks, key = { it.exercise.id }) { block ->
                    ExerciseCard(block, state.pounds, viewModel, Modifier.animateItem())
                }
                item {
                    PillButton(stringResource(R.string.workout_add_exercise), onClick = { picking = true }, icon = Icons.Rounded.Add,
                        modifier = Modifier.fillMaxWidth())
                }
                item {
                    TextButton(onClick = { confirmDiscard = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.workout_discard), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            AnimatedVisibility(
                visible = state.restEndsAt != null,
                enter = slideInVertically { it }, exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) { RestBar(state, viewModel) }
        }
    }

    if (picking) {
        ExercisePickerSheet(
            library = state.library,
            alreadyAdded = state.blocks.map { it.exercise.id }.toSet(),
            onPick = { picking = false; viewModel.addExercise(it) },
            onCreate = { n, m, k -> picking = false; viewModel.createExercise(n, m, k) },
            onDismiss = { picking = false },
        )
    }
    if (confirmFinish) {
        ConfirmDialog(
            title = stringResource(R.string.workout_finish_title),
            message = stringResource(R.string.workout_finish_body, state.doneSets),
            confirmLabel = stringResource(R.string.action_finish),
            onConfirm = { confirmFinish = false; viewModel.finish() },
            onDismiss = { confirmFinish = false },
        )
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.workout_discard_title),
            message = stringResource(R.string.workout_discard_body),
            confirmLabel = stringResource(R.string.action_discard),
            destructive = true,
            onConfirm = { confirmDiscard = false; viewModel.discard() },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
private fun SummaryStrip(state: WorkoutUiState) {
    val c = RaveTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(c.accentCard).padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Strip(Format.duration(state.elapsedS), stringResource(R.string.stat_time))
        Strip(state.doneSets.toString(), stringResource(R.string.workout_sets))
        Strip(formatWeight(state.volumeKg, state.pounds, 0) + " " + weightUnit(state.pounds), stringResource(R.string.workout_volume))
    }
}

@Composable
private fun Strip(value: String, label: String) {
    val c = RaveTheme.colors
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, color = c.onAccentCard)
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.onAccentCard.copy(alpha = 0.75f))
    }
}

@Composable
private fun ExerciseCard(block: ExerciseBlock, pounds: Boolean, viewModel: WorkoutViewModel, modifier: Modifier) {
    val ex = block.exercise
    var menu by remember { mutableStateOf(false) }
    SectionCard(modifier = modifier, contentPadding = PaddingValues(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ex.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(muscleRes(ex.muscle)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.workout_exercise_options)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.workout_remove_exercise)) }, onClick = { menu = false; viewModel.removeExercise(ex.id) })
                }
            }
        }
        // Column headers
        Row(Modifier.fillMaxWidth().padding(end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Header(stringResource(R.string.workout_col_set), Modifier.width(40.dp))
            Header(stringResource(R.string.workout_col_previous), Modifier.weight(1.4f))
            if (ex.kind == ExerciseKind.WEIGHT_REPS) Header(weightUnit(pounds), Modifier.weight(1f))
            Header(stringResource(if (ex.kind == ExerciseKind.DURATION) R.string.workout_col_seconds else R.string.workout_col_reps), Modifier.weight(1f))
            Spacer(Modifier.width(48.dp))
        }
        var working = 0
        block.sets.forEach { set ->
            val label = if (set.warmup) "W" else (++working).toString()
            key(set.id) { SetRow(set, label, block.previous.getOrNull(set.setOrder), ex.kind, pounds, viewModel) }
        }
        TextButton(onClick = { viewModel.addSet(ex.id) }) {
            Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.workout_add_set))
        }
    }
}

@Composable
private fun Header(text: String, modifier: Modifier) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = modifier)
}

@Composable
private fun SetRow(
    set: WorkoutSetEntity,
    label: String,
    previous: WorkoutSetEntity?,
    kind: ExerciseKind,
    pounds: Boolean,
    viewModel: WorkoutViewModel,
) {
    var menu by remember { mutableStateOf(false) }
    val doneColor = RaveTheme.colors.tileGreen
    Row(
        Modifier
            .fillMaxWidth()
            .padding(end = 8.dp, top = 3.dp, bottom = 3.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (set.done) doneColor.container else MaterialTheme.colorScheme.surface)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(40.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = if (set.warmup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { menu = true }.padding(vertical = 12.dp),
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(if (set.warmup) R.string.workout_mark_working else R.string.workout_mark_warmup)) },
                    onClick = { menu = false; viewModel.toggleWarmup(set) },
                )
                DropdownMenuItem(text = { Text(stringResource(R.string.workout_delete_set)) }, onClick = { menu = false; viewModel.deleteSet(set) })
            }
        }
        Text(
            previous?.let { previousLabel(it, kind, pounds) } ?: "–",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1.4f),
        )
        when (kind) {
            ExerciseKind.WEIGHT_REPS -> {
                NumberCell(formatWeight(set.weightKg, pounds, 1, blankZero = true), decimal = true, Modifier.weight(1f),
                    stringResource(R.string.cd_weight)) { v -> v.replace(',', '.').toDoubleOrNull()?.let { viewModel.setWeight(set, it) } }
                NumberCell(if (set.reps > 0) set.reps.toString() else "", decimal = false, Modifier.weight(1f),
                    stringResource(R.string.cd_reps)) { v -> viewModel.setReps(set, v.toIntOrNull() ?: 0) }
            }
            ExerciseKind.BODYWEIGHT_REPS -> NumberCell(if (set.reps > 0) set.reps.toString() else "", decimal = false, Modifier.weight(1f),
                stringResource(R.string.cd_reps)) { v -> viewModel.setReps(set, v.toIntOrNull() ?: 0) }
            ExerciseKind.DURATION -> NumberCell(if (set.durationS > 0) set.durationS.toString() else "", decimal = false, Modifier.weight(1f),
                stringResource(R.string.cd_seconds)) { v -> viewModel.setDuration(set, v.toIntOrNull() ?: 0) }
        }
        val cd = stringResource(if (set.done) R.string.cd_set_undo else R.string.cd_set_done, label)
        Box(
            Modifier.size(48.dp).clickable { viewModel.toggleDone(set) }.semantics { contentDescription = cd },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(30.dp).clip(CircleShape)
                    .background(if (set.done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, null, tint = if (set.done) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Compact numeric input; keeps its own text so typing "62." isn't reformatted mid-edit. */
@Composable
private fun NumberCell(initial: String, decimal: Boolean, modifier: Modifier, description: String, onChange: (String) -> Unit) {
    var text by remember(initial.isEmpty()) { mutableStateOf(initial) }
    Box(modifier.padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        BasicTextField(
            value = text,
            onValueChange = { v ->
                val clean = v.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }.take(6)
                text = clean
                onChange(clean)
            },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = MaterialTheme.typography.titleSmall.fontFamily, fontWeight = FontWeight.SemiBold,
                fontSize = MaterialTheme.typography.titleSmall.fontSize, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 10.dp)
                .semantics { contentDescription = description },
        )
    }
}

@Composable
private fun RestBar(state: WorkoutUiState, viewModel: WorkoutViewModel) {
    Surface(
        color = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timer, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.workout_rest, Format.duration(state.restLeftS.toLong())), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = { viewModel.adjustRest(-15) }) { Text("−15", color = MaterialTheme.colorScheme.onSecondary) }
                TextButton(onClick = { viewModel.adjustRest(15) }) { Text("+15", color = MaterialTheme.colorScheme.onSecondary) }
                Surface(onClick = viewModel::skipRest, shape = PillShape, color = MaterialTheme.colorScheme.primary) {
                    Text(stringResource(R.string.action_skip), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            PillProgress(if (state.restTotalS == 0) 0f else 1f - state.restLeftS / state.restTotalS.toFloat())
        }
    }
}

// ---- Formatting helpers ----

@Composable
fun weightUnit(pounds: Boolean): String = stringResource(if (pounds) R.string.unit_lb else R.string.unit_kg)

fun formatWeight(kg: Double, pounds: Boolean, decimals: Int, blankZero: Boolean = false): String {
    if (blankZero && kg <= 0.0) return ""
    val v = if (pounds) kg * com.rave.app.domain.workout.WorkoutMath.LB_PER_KG else kg
    val s = String.format(Locale.US, "%.${decimals}f", v)
    return if (decimals > 0) s.trimEnd('0').trimEnd('.') else s
}

@Composable
fun previousLabel(s: WorkoutSetEntity, kind: ExerciseKind, pounds: Boolean): String = when (kind) {
    ExerciseKind.WEIGHT_REPS -> "${formatWeight(s.weightKg, pounds, 1)} ${weightUnit(pounds)} × ${s.reps}"
    ExerciseKind.BODYWEIGHT_REPS -> "× ${s.reps}"
    ExerciseKind.DURATION -> Format.duration(s.durationS.toLong())
}
