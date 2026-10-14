package com.rave.app.ui.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.rave.app.core.TimeProvider
import com.rave.app.data.food.FoodRepository
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.food.DailyTarget
import com.rave.app.domain.food.DayPlan
import com.rave.app.domain.food.FoodSource
import com.rave.app.domain.food.MealPlanner
import com.rave.app.domain.food.PlannedMeal
import com.rave.app.domain.food.PlannerOptions
import com.rave.app.domain.model.UserSettings
import com.rave.app.ui.components.PillChip
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.TonalPillButton
import com.rave.app.ui.theme.Dimens
import com.rave.app.ui.theme.RaveTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

data class MealPlanUi(
    val target: DailyTarget? = null,
    val settings: UserSettings = UserSettings(),
    val week: List<DayPlan> = emptyList(),
    val groceries: List<Pair<String, Int>> = emptyList(),
    val checked: Set<String> = emptySet(),
)

@HiltViewModel
class MealPlanViewModel @Inject constructor(
    private val food: FoodRepository,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {
    private val weekStart = time.weekStart()
    private val seedKey = "meal_seed_$weekStart"
    private val checkedKey = "meal_checked_$weekStart"
    private val seed = MutableStateFlow<Long?>(null)
    private val checked = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            seed.value = settings.getString(seedKey)?.toLongOrNull() ?: weekStart.toEpochDay()
            checked.value = settings.getString(checkedKey).orEmpty().split('|').filter { it.isNotBlank() }.toSet()
        }
    }

    val ui: StateFlow<MealPlanUi> = combine(food.observeTarget(), settings.settings, seed, checked) { target, s, sd, chk ->
        if (sd == null) return@combine MealPlanUi(target, s)
        val options = PlannerOptions(
            dailyKcal = target.kcal, vegetarian = s.food.vegetarian, ramadan = s.ramadanMeals, goal = s.food.goal, budget = s.budgetMeals,
        )
        val week = MealPlanner.week(weekStart, options, sd)
        MealPlanUi(target, s, week, MealPlanner.groceries(week), chk)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MealPlanUi())

    fun regenerate() = viewModelScope.launch {
        val next = Random.nextLong()
        settings.putString(seedKey, next.toString())
        settings.putString(checkedKey, null)
        checked.value = emptySet()
        seed.value = next
    }

    fun toggle(item: String) = viewModelScope.launch {
        val next = if (item in checked.value) checked.value - item else checked.value + item
        checked.value = next
        settings.putString(checkedKey, next.joinToString("|"))
    }

    fun ate(meal: PlannedMeal) = viewModelScope.launch {
        meal.items.forEach { i ->
            food.log("${if (i.servings > 1) "${i.servings} × " else ""}${i.food.name}", i.kcal, i.food.protein * i.servings,
                i.food.carbs * i.servings, i.food.fat * i.servings, null, FoodSource.PLAN, meal.slot)
        }
    }

    fun todayIndex(): Int = (time.today().toEpochDay() - weekStart.toEpochDay()).toInt().coerceIn(0, 6)
}

@Composable
fun MealPlanScreen(onBack: () -> Unit, onOpenSettings: () -> Unit, viewModel: MealPlanViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var day by rememberSaveable { mutableIntStateOf(viewModel.todayIndex()) }
    var eaten by rememberSaveable { mutableIntStateOf(-1) }
    RaveScreen(
        title = stringResource(R.string.plan_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = viewModel::regenerate) { Icon(Icons.Rounded.Refresh, stringResource(R.string.plan_new)) }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, stringResource(R.string.food_settings)) }
        },
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = Dimens.ScreenPadding)) {
            ui.target?.let { t ->
                Text(stringResource(R.string.plan_target, "%,d".format(t.kcal), goalName(ui.settings.food.goal)),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip(tab == 0, stringResource(R.string.plan_tab_plan), { tab = 0 })
                PillChip(tab == 1, stringResource(R.string.plan_tab_groceries), { tab = 1 })
            }
            if (tab == 0) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(ui.week) { i, d ->
                        PillChip(day == i, d.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + d.date.dayOfMonth,
                            { day = i; eaten = -1 })
                    }
                }
                val plan = ui.week.getOrNull(day)
                LazyColumn(Modifier.fillMaxSize().navigationBarsPadding().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap)) {
                    if (plan != null) {
                        item { Text(stringResource(R.string.plan_day_total, "%,d".format(plan.kcal), plan.protein.toInt()), style = MaterialTheme.typography.titleSmall) }
                        itemsIndexed(plan.meals) { i, meal ->
                            MealCard(meal, done = eaten == day * 10 + i) { viewModel.ate(meal); eaten = day * 10 + i }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().navigationBarsPadding()) {
                    item { Text(stringResource(R.string.plan_groceries_body), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp)) }
                    items(ui.groceries, key = { it.first }) { (name, count) ->
                        Row(Modifier.fillMaxWidth().clickable { viewModel.toggle(name) }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = name in ui.checked, onCheckedChange = { viewModel.toggle(name) })
                            Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(stringResource(R.string.plan_used_in, count), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MealCard(meal: PlannedMeal, done: Boolean, onAte: () -> Unit) {
    SectionCard(Modifier.fillMaxWidth()) {
        Row {
            Text(slotName(meal.slot), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.food_kcal, "%,d".format(meal.kcal)), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        meal.items.forEach { i ->
            Row(Modifier.padding(top = 6.dp)) {
                Column(Modifier.weight(1f)) {
                    Text((if (i.servings > 1) "${i.servings} × " else "") + i.food.name, style = MaterialTheme.typography.bodyMedium)
                    Text(i.food.serving, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${i.kcal}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (done) {
            Text(stringResource(R.string.food_logged, slotName(meal.slot)), style = MaterialTheme.typography.labelLarge, color = RaveTheme.colors.verdictGo.content)
        } else {
            TonalPillButton(stringResource(R.string.plan_ate_this), onClick = onAte, modifier = Modifier.fillMaxWidth())
        }
    }
}
