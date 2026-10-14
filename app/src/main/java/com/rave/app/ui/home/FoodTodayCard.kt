package com.rave.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.rave.app.data.food.FoodRepository
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.StepRepository
import com.rave.app.domain.fitness.FitnessMath
import com.rave.app.ui.components.IconTile
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.food.FoodUiState
import com.rave.app.ui.theme.RaveTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.NumberFormat
import javax.inject.Inject

@HiltViewModel
class FoodTodayViewModel @Inject constructor(
    food: FoodRepository,
    steps: StepRepository,
    settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<FoodUiState> = combine(
        food.observeToday(), food.observeTarget(), steps.observeToday(), settings.settings,
    ) { day, target, stepsToday, s ->
        FoodUiState(day, target, FitnessMath.stepCalories(stepsToday, s.weightKg, s.heightCm))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoodUiState())
}

/** Home: one slim row with calories eaten vs today's target. Tap opens the Scan tab. */
@Composable
fun FoodTodayCard(onOpenFood: () -> Unit, viewModel: FoodTodayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val target = state.target ?: return
    val eaten = state.day.kcal
    val over = eaten > target.kcal
    val accent = if (over) RaveTheme.colors.verdictSkip.content else MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val progress = (eaten / target.kcal.toFloat()).coerceIn(0f, 1f)
    val nf = NumberFormat.getIntegerInstance()
    SectionCard(Modifier.fillMaxWidth(), onClick = onOpenFood) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Rounded.RestaurantMenu, RaveTheme.colors.tileGreen)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.food_today_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.home_food_summary, nf.format(eaten), nf.format(target.kcal)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(44.dp)) {
                    val stroke = 5.dp.toPx()
                    val inset = Offset(stroke / 2, stroke / 2)
                    val arc = Size(size.width - stroke, size.height - stroke)
                    drawArc(track, 0f, 360f, false, inset, arc, style = Stroke(stroke))
                    if (progress > 0f) drawArc(accent, -90f, 360f * progress, false, inset, arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
