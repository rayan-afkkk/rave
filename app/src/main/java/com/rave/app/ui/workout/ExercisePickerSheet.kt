package com.rave.app.ui.workout

import androidx.annotation.StringRes
import com.rave.app.ui.theme.Dimens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rave.app.R
import com.rave.app.domain.workout.Exercise
import com.rave.app.domain.workout.ExerciseKind
import com.rave.app.domain.workout.MuscleGroup
import com.rave.app.ui.components.IconTile
import com.rave.app.ui.components.PillChip
import com.rave.app.ui.theme.RaveTheme
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.size
import com.rave.app.ui.workout.guide.ExerciseGuide
import com.rave.app.ui.workout.guide.ExerciseGuideSheet
import com.rave.app.ui.workout.guide.ExerciseThumb
import com.rave.app.ui.workout.guide.rememberGuide
import com.rave.app.ui.workout.guide.guideFor
import com.rave.app.ui.workout.guide.needsNoEquipment

@StringRes
fun muscleRes(m: MuscleGroup): Int = when (m) {
    MuscleGroup.CHEST -> R.string.muscle_chest
    MuscleGroup.BACK -> R.string.muscle_back
    MuscleGroup.LEGS -> R.string.muscle_legs
    MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
    MuscleGroup.ARMS -> R.string.muscle_arms
    MuscleGroup.CORE -> R.string.muscle_core
    MuscleGroup.FULL_BODY -> R.string.muscle_full_body
}

@StringRes
fun kindRes(k: ExerciseKind): Int = when (k) {
    ExerciseKind.WEIGHT_REPS -> R.string.kind_weight_reps
    ExerciseKind.BODYWEIGHT_REPS -> R.string.kind_bodyweight
    ExerciseKind.DURATION -> R.string.kind_duration
}

/** Searchable exercise library with muscle filters and "create exercise". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    library: List<Exercise>,
    alreadyAdded: Set<Long>,
    onPick: (Exercise) -> Unit,
    onCreate: (String, MuscleGroup, ExerciseKind) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf<MuscleGroup?>(null) }
    var noEquipment by rememberSaveable { mutableStateOf(false) }
    val res = androidx.compose.ui.platform.LocalContext.current.resources
    val bodyweight = remember(library) { library.filter { needsNoEquipment(guideFor(res, it)) }.map { it.id }.toSet() }
    var creating by rememberSaveable { mutableStateOf(false) }
    var info by remember { mutableStateOf<Pair<ExerciseGuide, MuscleGroup>?>(null) }
    val filtered = remember(library, query, muscle, noEquipment, bodyweight) {
        library.filter {
            (muscle == null || it.muscle == muscle) && (!noEquipment || it.id in bodyweight) && it.name.contains(query.trim(), ignoreCase = true)
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Dimens.ScreenPadding)) {
            Text(stringResource(R.string.workout_add_exercise), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it.take(40) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) }, placeholder = { Text(stringResource(R.string.workout_search)) },
                singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { PillChip(muscle == null && !noEquipment, stringResource(R.string.filter_all), { muscle = null; noEquipment = false }) }
                item { PillChip(noEquipment, stringResource(R.string.workout_no_equipment), { noEquipment = !noEquipment }) }
                items(MuscleGroup.entries) { m -> PillChip(muscle == m, stringResource(muscleRes(m)), { muscle = m }) }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(filtered, key = { it.id }) { ex ->
                    val added = ex.id in alreadyAdded
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = !added) { onPick(ex) }.heightIn(min = 56.dp).padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val guide = rememberGuide(ex)
                        if (guide != null) ExerciseThumb(guide, ex.muscle, Modifier.size(48.dp))
                        else IconTile(Icons.Rounded.FitnessCenter, RaveTheme.colors.tiles[ex.muscle.ordinal % RaveTheme.colors.tiles.size], size = 48.dp, iconSize = 22.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ex.name, style = MaterialTheme.typography.bodyLarge)
                            Text("${stringResource(muscleRes(ex.muscle))} · ${stringResource(kindRes(ex.kind))}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (added) Icon(Icons.Rounded.Check, stringResource(R.string.workout_already_added), tint = MaterialTheme.colorScheme.tertiary)
                        if (guide != null) {
                            IconButton(onClick = { info = guide to ex.muscle }) {
                                Icon(Icons.Rounded.Info, stringResource(R.string.guide_open, ex.name), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = { creating = true }) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (query.isBlank()) stringResource(R.string.workout_create_exercise) else stringResource(R.string.workout_create_named, query.trim()))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    info?.let { (guide, group) -> ExerciseGuideSheet(guide, group, onDismiss = { info = null }) }
    if (creating) {
        CreateExerciseDialog(initialName = query.trim(), onDismiss = { creating = false }) { n, m, k ->
            creating = false
            onCreate(n, m, k)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateExerciseDialog(initialName: String, onDismiss: () -> Unit, onCreate: (String, MuscleGroup, ExerciseKind) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var muscle by rememberSaveable { mutableStateOf(MuscleGroup.CHEST) }
    var kind by rememberSaveable { mutableStateOf(ExerciseKind.WEIGHT_REPS) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workout_create_exercise)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(40) }, label = { Text(stringResource(R.string.habit_name)) }, singleLine = true)
                Text(stringResource(R.string.workout_muscle), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MuscleGroup.entries.forEach { m -> PillChip(muscle == m, stringResource(muscleRes(m)), { muscle = m }) }
                }
                Text(stringResource(R.string.workout_measure), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExerciseKind.entries.forEach { k -> PillChip(kind == k, stringResource(kindRes(k)), { kind = k }) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(name, muscle, kind) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.action_add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
