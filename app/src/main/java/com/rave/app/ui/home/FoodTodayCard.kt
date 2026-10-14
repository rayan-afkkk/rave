package com.rave.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.rave.app.ui.components.TonalPillButton
import com.rave.app.ui.food.CaloriesCard
import com.rave.app.ui.food.FoodUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

/** Home: calories eaten vs today's target, with shortcuts to scan a snack or open the food hub. */
@Composable
fun FoodTodayCard(onOpenFood: () -> Unit, onScanFood: () -> Unit, viewModel: FoodTodayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CaloriesCard(state, onClick = onOpenFood) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TonalPillButton(stringResource(R.string.food_scan_barcode), onScanFood, Modifier.weight(1f), icon = Icons.Rounded.QrCodeScanner)
            TonalPillButton(stringResource(R.string.food_title), onOpenFood, Modifier.weight(1f), icon = Icons.Rounded.RestaurantMenu)
        }
    }
}
