package com.rave.app.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.rave.app.data.food.FoodRepository
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.food.Allergen
import com.rave.app.domain.food.DailyTarget
import com.rave.app.domain.food.FoodGoal
import com.rave.app.domain.food.Sex
import com.rave.app.domain.model.UserSettings
import com.rave.app.ui.components.PillChip
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.theme.Dimens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoodSettingsViewModel @Inject constructor(private val settings: SettingsRepository, food: FoodRepository) : ViewModel() {
    val state: StateFlow<UserSettings?> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val target: StateFlow<DailyTarget?> = food.observeTarget().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun update(transform: (UserSettings) -> UserSettings) = viewModelScope.launch { settings.update(transform) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodSettingsScreen(onBack: () -> Unit, viewModel: FoodSettingsViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val target by viewModel.target.collectAsStateWithLifecycle()
    val st = s ?: return
    RaveScreen(title = stringResource(R.string.food_settings_title), onBack = onBack) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
        ) {
            target?.let { t ->
                Text(stringResource(R.string.food_target_now, "%,d".format(t.kcal), t.proteinG, t.carbsG, t.fatG),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            SectionCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.food_goal), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FoodGoal.entries.forEach { g -> PillChip(st.food.goal == g, goalName(g), { viewModel.update { it.copy(food = it.food.copy(goal = g)) } }) }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.food_sex), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(Sex.MALE to R.string.sex_male, Sex.FEMALE to R.string.sex_female, Sex.UNSPECIFIED to R.string.sex_unspecified).forEach { (sex, label) ->
                        PillChip(st.sex == sex, stringResource(label), { viewModel.update { it.copy(sex = sex) } })
                    }
                }
                Spacer(Modifier.height(12.dp))
                var year by rememberSaveable { mutableStateOf(if (st.birthYear > 0) st.birthYear.toString() else "") }
                OutlinedTextField(
                    value = year,
                    onValueChange = { v ->
                        year = v.filter(Char::isDigit).take(4)
                        year.toIntOrNull()?.takeIf { it in 1930..2015 }?.let { y -> viewModel.update { it.copy(birthYear = y) } }
                    },
                    label = { Text(stringResource(R.string.food_birth_year)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
            }
            SectionCard(Modifier.fillMaxWidth()) {
                ToggleRow(stringResource(R.string.food_halal_only), stringResource(R.string.food_halal_help), st.food.halalOnly) { on ->
                    viewModel.update { it.copy(food = it.food.copy(halalOnly = on)) }
                }
                ToggleRow(stringResource(R.string.food_vegetarian), null, st.food.vegetarian) { on ->
                    viewModel.update { it.copy(food = it.food.copy(vegetarian = on)) }
                }
                ToggleRow(stringResource(R.string.food_budget), stringResource(R.string.food_budget_help), st.budgetMeals) { on ->
                    viewModel.update { it.copy(budgetMeals = on) }
                }
            }
            SectionCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.food_allergies), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Allergen.entries.forEach { a ->
                        val on = a in st.food.allergies
                        PillChip(on, allergenName(a), {
                            viewModel.update { it.copy(food = it.food.copy(allergies = if (on) it.food.allergies - a else it.food.allergies + a)) }
                        })
                    }
                }
            }
            Text(stringResource(R.string.food_disclaimer), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ToggleRow(title: String, help: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            help?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
