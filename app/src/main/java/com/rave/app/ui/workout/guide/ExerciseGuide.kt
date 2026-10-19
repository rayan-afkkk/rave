package com.rave.app.ui.workout.guide

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rave.app.R
import com.rave.app.domain.workout.Exercise
import com.rave.app.domain.workout.MuscleGroup
import com.rave.app.ui.components.BentoChip
import com.rave.app.ui.components.BentoKicker
import com.rave.app.ui.components.BentoTileShape
import com.rave.app.ui.components.BentoTitle
import com.rave.app.ui.theme.FigureStageBlue
import com.rave.app.ui.theme.FigureStageGreen
import com.rave.app.ui.theme.FigureStagePeach
import com.rave.app.ui.theme.FigureStagePink
import com.rave.app.ui.theme.FigureStageYellow
import com.rave.app.ui.theme.RaveTheme

/** How-to content for a built-in exercise. */
class ExerciseGuide(
    val key: String,
    @StringRes val name: Int,
    @ArrayRes val steps: Int,
    @ArrayRes val avoid: Int,
) {
    val move: Move get() = MOVES.getValue(key)
}

val EXERCISE_GUIDES: List<ExerciseGuide> = listOf(
    ExerciseGuide("bench_press", R.string.ex_bench_press, R.array.guide_bench_press_steps, R.array.guide_bench_press_avoid),
    ExerciseGuide("incline_db_press", R.string.ex_incline_db_press, R.array.guide_incline_db_press_steps, R.array.guide_incline_db_press_avoid),
    ExerciseGuide("push_up", R.string.ex_push_up, R.array.guide_push_up_steps, R.array.guide_push_up_avoid),
    ExerciseGuide("deadlift", R.string.ex_deadlift, R.array.guide_deadlift_steps, R.array.guide_deadlift_avoid),
    ExerciseGuide("barbell_row", R.string.ex_barbell_row, R.array.guide_barbell_row_steps, R.array.guide_barbell_row_avoid),
    ExerciseGuide("lat_pulldown", R.string.ex_lat_pulldown, R.array.guide_lat_pulldown_steps, R.array.guide_lat_pulldown_avoid),
    ExerciseGuide("pull_up", R.string.ex_pull_up, R.array.guide_pull_up_steps, R.array.guide_pull_up_avoid),
    ExerciseGuide("squat", R.string.ex_squat, R.array.guide_squat_steps, R.array.guide_squat_avoid),
    ExerciseGuide("leg_press", R.string.ex_leg_press, R.array.guide_leg_press_steps, R.array.guide_leg_press_avoid),
    ExerciseGuide("lunge", R.string.ex_lunge, R.array.guide_lunge_steps, R.array.guide_lunge_avoid),
    ExerciseGuide("rdl", R.string.ex_rdl, R.array.guide_rdl_steps, R.array.guide_rdl_avoid),
    ExerciseGuide("calf_raise", R.string.ex_calf_raise, R.array.guide_calf_raise_steps, R.array.guide_calf_raise_avoid),
    ExerciseGuide("ohp", R.string.ex_ohp, R.array.guide_ohp_steps, R.array.guide_ohp_avoid),
    ExerciseGuide("lateral_raise", R.string.ex_lateral_raise, R.array.guide_lateral_raise_steps, R.array.guide_lateral_raise_avoid),
    ExerciseGuide("bicep_curl", R.string.ex_bicep_curl, R.array.guide_bicep_curl_steps, R.array.guide_bicep_curl_avoid),
    ExerciseGuide("tricep_pushdown", R.string.ex_tricep_pushdown, R.array.guide_tricep_pushdown_steps, R.array.guide_tricep_pushdown_avoid),
    ExerciseGuide("dip", R.string.ex_dip, R.array.guide_dip_steps, R.array.guide_dip_avoid),
    ExerciseGuide("plank", R.string.ex_plank, R.array.guide_plank_steps, R.array.guide_plank_avoid),
    ExerciseGuide("crunch", R.string.ex_crunch, R.array.guide_crunch_steps, R.array.guide_crunch_avoid),
    ExerciseGuide("kettlebell_swing", R.string.ex_kettlebell_swing, R.array.guide_kettlebell_swing_steps, R.array.guide_kettlebell_swing_avoid),
    ExerciseGuide("burpee", R.string.ex_burpee, R.array.guide_burpee_steps, R.array.guide_burpee_avoid),
    ExerciseGuide("bodyweight_squat", R.string.ex_bodyweight_squat, R.array.guide_bodyweight_squat_steps, R.array.guide_bodyweight_squat_avoid),
    ExerciseGuide("jumping_jack", R.string.ex_jumping_jack, R.array.guide_jumping_jack_steps, R.array.guide_jumping_jack_avoid),
    ExerciseGuide("mountain_climber", R.string.ex_mountain_climber, R.array.guide_mountain_climber_steps, R.array.guide_mountain_climber_avoid),
    ExerciseGuide("glute_bridge", R.string.ex_glute_bridge, R.array.guide_glute_bridge_steps, R.array.guide_glute_bridge_avoid),
    ExerciseGuide("wall_sit", R.string.ex_wall_sit, R.array.guide_wall_sit_steps, R.array.guide_wall_sit_avoid),
    ExerciseGuide("leg_raise", R.string.ex_leg_raise, R.array.guide_leg_raise_steps, R.array.guide_leg_raise_avoid),
    ExerciseGuide("high_knees", R.string.ex_high_knees, R.array.guide_high_knees_steps, R.array.guide_high_knees_avoid),
    ExerciseGuide("superman", R.string.ex_superman, R.array.guide_superman_steps, R.array.guide_superman_avoid),
    ExerciseGuide("pike_push_up", R.string.ex_pike_push_up, R.array.guide_pike_push_up_steps, R.array.guide_pike_push_up_avoid),
    ExerciseGuide("bodyweight_lunge", R.string.ex_bodyweight_lunge, R.array.guide_bodyweight_lunge_steps, R.array.guide_bodyweight_lunge_avoid),
)

/**
 * The guide for an exercise, matched by name (built-in exercises are stored with their localized
 * name). Custom exercises have none.
 */
@Composable
fun rememberGuide(exercise: Exercise): ExerciseGuide? {
    val res = LocalContext.current.resources
    return remember(exercise.name, exercise.custom) { guideFor(res, exercise) }
}

fun guideFor(res: android.content.res.Resources, exercise: Exercise): ExerciseGuide? =
    if (exercise.custom) null
    else EXERCISE_GUIDES.firstOrNull { res.getString(it.name).equals(exercise.name.trim(), ignoreCase = true) }

/** Built-in exercises that need nothing but the floor (a mat or a wall at most). */
fun needsNoEquipment(guide: ExerciseGuide?): Boolean =
    guide != null && guide.move.gear.all { it == Gear.MAT || it == Gear.WALL }

/** Light backdrop the figure stands on, picked by muscle group. */
fun figureStage(group: MuscleGroup): Color = when (group) {
    MuscleGroup.CHEST -> FigureStagePeach
    MuscleGroup.BACK -> FigureStageBlue
    MuscleGroup.LEGS -> FigureStageGreen
    MuscleGroup.SHOULDERS -> FigureStagePink
    MuscleGroup.ARMS -> FigureStageYellow
    MuscleGroup.CORE -> FigureStageBlue
    MuscleGroup.FULL_BODY -> FigureStagePeach
}

/** Small still of the move for list rows. */
@Composable
fun ExerciseThumb(guide: ExerciseGuide, group: MuscleGroup, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape12).background(figureStage(group))) {
        ExerciseFigure(guide.move, Modifier.fillMaxSize(), animate = false, glow = false)
    }
}

private val RoundedCornerShape12 = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseGuideSheet(guide: ExerciseGuide, group: MuscleGroup, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        ExerciseGuideContent(guide, group, Modifier.navigationBarsPadding().verticalScroll(rememberScrollState()))
    }
}

/** Animated figure, muscles worked, how-to steps and common mistakes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseGuideContent(guide: ExerciseGuide, group: MuscleGroup, modifier: Modifier = Modifier) {
    val c = RaveTheme.colors
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Box(Modifier.fillMaxWidth().height(250.dp).clip(BentoTileShape).background(figureStage(group))) {
            ExerciseFigure(guide.move, Modifier.fillMaxSize().padding(10.dp))
        }
        Spacer(Modifier.height(14.dp))
        BentoTitle(stringResource(guide.name), color = onSurface)
        Spacer(Modifier.height(10.dp))
        BentoKicker(stringResource(R.string.guide_muscles), onSurface)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            guide.move.muscles.forEach { BentoChip(stringResource(it.label), c.tilePeach.container, c.tilePeach.content) }
        }
        Spacer(Modifier.height(18.dp))
        BentoKicker(stringResource(R.string.guide_how_to), onSurface)
        Spacer(Modifier.height(8.dp))
        stringArrayResource(guide.steps).forEachIndexed { i, step ->
            Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Text((i + 1).toString(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Text(step, style = MaterialTheme.typography.bodyLarge, color = onSurface, modifier = Modifier.weight(1f).padding(top = 2.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        BentoKicker(stringResource(R.string.guide_avoid), onSurface)
        Spacer(Modifier.height(8.dp))
        stringArrayResource(guide.avoid).forEach { tip ->
            Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(tip, style = MaterialTheme.typography.bodyLarge, color = onSurface, modifier = Modifier.weight(1f).padding(top = 2.dp))
            }
        }
    }
}
