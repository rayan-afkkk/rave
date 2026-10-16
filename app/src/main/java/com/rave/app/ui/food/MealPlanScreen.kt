package com.rave.app.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
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
)

@HiltViewModel
class MealPlanViewModel @Inject constructor(
    private val food: FoodRepository,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
    pro: com.rave.app.data.billing.ProRepository,
) : ViewModel() {
    private val weekStart = time.weekStart()
    private val seedKey = "meal_seed_$weekStart"
    private val seed = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            seed.value = settings.getString(seedKey)?.toLongOrNull() ?: weekStart.toEpochDay()
        }
    }

    val ui: StateFlow<MealPlanUi> = combine(food.observeTarget(), settings.settings, seed, pro.isPro) { target, s, sd, isPro ->
        if (sd == null) return@combine MealPlanUi(target, s)
        // Ramadan mode is a Pro feature; a setting saved earlier is ignored without Pro.
        val options = PlannerOptions(
            dailyKcal = target.kcal, vegetarian = s.food.vegetarian, ramadan = s.ramadanMeals && isPro, goal = s.food.goal, budget = s.budgetMeals,
        )
        val week = MealPlanner.week(weekStart, options, sd)
        MealPlanUi(target, s, week)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MealPlanUi())

    fun regenerate() = viewModelScope.launch {
        val next = Random.nextLong()
        settings.putString(seedKey, next.toString())
        seed.value = next
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
    val today = remember { viewModel.todayIndex() }
    var day by rememberSaveable { mutableIntStateOf(today) }
    val pro = com.rave.app.ui.pro.rememberPro()
    // Free: today's plan only. Pro: the whole week.
    if (!pro.isPro && day != today) day = today
    var logged by rememberSaveable { mutableStateOf(setOf<Int>()) }
    RaveScreen(
        title = stringResource(R.string.plan_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = { pro.gate(com.rave.app.data.billing.ProReason.MEAL_PLAN) { viewModel.regenerate() } }) { Icon(Icons.Rounded.Refresh, stringResource(R.string.plan_new)) }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Tune, stringResource(R.string.food_settings)) }
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ui.week.forEachIndexed { i, d ->
                        DayPill(d, selected = i == day, isToday = i == today, locked = !pro.isPro && i != today, modifier = Modifier.weight(1f)) {
                            if (i == today) day = i else pro.gate(com.rave.app.data.billing.ProReason.MEAL_PLAN) { day = i }
                        }
                    }
                }
            }
            val plan = ui.week.getOrNull(day)
            if (plan != null) {
                item {
                    Column(Modifier.padding(horizontal = 4.dp)) {
                        Text(stringResource(R.string.food_kcal, "%,d".format(plan.kcal)), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                        ui.target?.let { t ->
                            Text(stringResource(R.string.plan_target, "%,d".format(t.kcal), goalName(ui.settings.food.goal)),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    SectionCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                        plan.meals.forEachIndexed { i, meal ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            val key = day * 10 + i
                            MealRow(meal, done = key in logged) { viewModel.ate(meal); logged = logged + key }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayPill(day: DayPlan, selected: Boolean, isToday: Boolean, locked: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(
        modifier.clip(MaterialTheme.shapes.medium).background(bg).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall,
            color = if (selected) fg else MaterialTheme.colorScheme.onSurfaceVariant)
        if (locked) {
            Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        } else {
            Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = fg)
        }
        Box(Modifier.padding(top = 2.dp).size(4.dp).clip(CircleShape).background(if (isToday) (if (selected) fg else MaterialTheme.colorScheme.primary) else bg))
    }
}

/** One meal: slot, the dishes on one line, calories, and a tick to log it. */
@Composable
private fun MealRow(meal: PlannedMeal, done: Boolean, onAte: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(slotName(meal.slot).uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                meal.items.joinToString(" + ") { (if (it.servings > 1) "${it.servings} " else "") + it.food.name },
                style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(stringResource(R.string.food_kcal, "%,d".format(meal.kcal)), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(8.dp))
        val tint = if (done) RaveTheme.colors.verdictGo else RaveTheme.colors.tilePeach
        IconButton(onClick = onAte, enabled = !done, modifier = Modifier.clip(CircleShape).background(tint.container).size(40.dp)) {
            Icon(if (done) Icons.Rounded.Check else Icons.Rounded.Add,
                contentDescription = if (done) stringResource(R.string.food_logged, slotName(meal.slot)) else stringResource(R.string.plan_ate_this),
                tint = tint.content)
        }
    }
}
