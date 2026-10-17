package com.rave.app.ui.food

import androidx.compose.foundation.background
import com.rave.app.ui.components.SmoothCornerShape
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.rave.app.domain.food.MealSlot
import com.rave.app.domain.food.PlannedMeal
import com.rave.app.domain.food.PlannerOptions
import com.rave.app.domain.model.UserSettings
import com.rave.app.ui.components.AppBackground
import com.rave.app.ui.components.BentoChip
import com.rave.app.ui.components.BentoGap
import com.rave.app.ui.components.BentoKicker
import com.rave.app.ui.components.BentoTile
import com.rave.app.ui.components.BentoTileShape
import com.rave.app.ui.components.BentoTitle
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
import kotlin.math.roundToInt
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
) : ViewModel() {
    private val weekStart = time.weekStart()
    private val seedKey = "meal_seed_$weekStart"
    private val seed = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            seed.value = settings.getString(seedKey)?.toLongOrNull() ?: weekStart.toEpochDay()
        }
    }

    val ui: StateFlow<MealPlanUi> = combine(food.observeTarget(), settings.settings, seed) { target, s, sd ->
        if (sd == null) return@combine MealPlanUi(target, s)
        val options = PlannerOptions(
            dailyKcal = target.kcal, vegetarian = s.food.vegetarian, goal = s.food.goal, budget = s.budgetMeals,
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


/*
 * Bento Pop weekly plan: big title, ink/sand day chips, a lime day summary tile and one
 * colour card per meal slot (breakfast peach, lunch sky, dinner lilac, snack sand).
 */
@Composable
fun MealPlanScreen(onBack: () -> Unit, onOpenSettings: () -> Unit, viewModel: MealPlanViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val today = remember { viewModel.todayIndex() }
    var day by rememberSaveable { mutableIntStateOf(today) }
    val pro = com.rave.app.ui.pro.rememberPro()
    // Free: today's plan only. Pro: the whole week.
    if (!pro.isPro && day != today) day = today
    var logged by rememberSaveable { mutableStateOf(setOf<Int>()) }
    val ink = MaterialTheme.colorScheme.onBackground
    AppBackground {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(BentoGap),
        ) {
            item {
                Column {
                    Row(Modifier.fillMaxWidth().offset(x = (-12).dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = ink)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { pro.gate(com.rave.app.data.billing.ProReason.MEAL_PLAN) { viewModel.regenerate() } }, modifier = Modifier.offset(x = 19.dp).size(38.dp)) {
                            Icon(Icons.Rounded.Refresh, stringResource(R.string.plan_new), tint = ink)
                        }
                        IconButton(onClick = onOpenSettings, modifier = Modifier.offset(x = 19.dp).size(38.dp)) {
                            Icon(Icons.Rounded.Tune, stringResource(R.string.food_settings), tint = ink)
                        }
                    }
                    BentoTitle(stringResource(R.string.bplan_title), Modifier.padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ui.week.forEachIndexed { i, d ->
                            DayChip(d, selected = i == day, locked = !pro.isPro && i != today, modifier = Modifier.weight(1f)) {
                                if (i == today) day = i else pro.gate(com.rave.app.data.billing.ProReason.MEAL_PLAN) { day = i }
                            }
                        }
                    }
                }
            }
            val plan = ui.week.getOrNull(day)
            if (plan != null) {
                item { DaySummary(plan) }
                plan.meals.forEachIndexed { i, meal ->
                    item(key = "meal_${day}_$i") {
                        val key = day * 10 + i
                        MealCard(meal, done = key in logged) { viewModel.ate(meal); logged = logged + key }
                    }
                }
            }
        }
    }
}

/** Rounded day chip: weekday letter + date; ink when selected, sand otherwise, lock when Pro-only. */
@Composable
private fun DayChip(day: DayPlan, selected: Boolean, locked: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tile = if (selected) RaveTheme.colors.tileInk else RaveTheme.colors.tileSand
    Column(
        modifier.height(58.dp).clip(SmoothCornerShape(16.dp)).background(tile.container).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Normal),
            color = tile.content.copy(alpha = 0.7f), maxLines = 1,
        )
        if (locked) {
            Icon(Icons.Rounded.Lock, null, tint = tile.content.copy(alpha = 0.7f), modifier = Modifier.padding(top = 2.dp).size(16.dp))
        } else {
            Text(
                day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
                color = tile.content, maxLines = 1,
            )
        }
    }
}

/** Lime tile: weekday kicker, big day calories and an ink protein chip. */
@Composable
private fun DaySummary(plan: DayPlan) {
    val tile = RaveTheme.colors.tileGreen
    BentoTile(tile.container, Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BentoKicker(plan.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()), tile.content)
                val unit = stringResource(R.string.bplan_kcal_unit)
                Text(
                    buildAnnotatedString {
                        append("%,d".format(plan.kcal))
                        withStyle(SpanStyle(fontSize = 16.sp, letterSpacing = (-0.5).sp)) { append(" $unit") }
                    },
                    color = tile.content, maxLines = 1, modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp, lineHeight = 28.sp,
                    ),
                )
            }
            BentoChip(
                stringResource(R.string.bplan_protein, plan.protein.roundToInt()),
                container = RaveTheme.colors.tileInk.container, content = RaveTheme.colors.tileInk.content,
            )
        }
    }
}

/** One meal slot as a colour card: emoji, slot kicker, dishes, calories and a log/check button. */
@Composable
private fun MealCard(meal: PlannedMeal, done: Boolean, onAte: () -> Unit) {
    val c = RaveTheme.colors
    val tile = when (meal.slot) {
        MealSlot.BREAKFAST -> c.tilePeach
        MealSlot.LUNCH -> c.tileBlue
        MealSlot.DINNER -> c.tilePink
        MealSlot.SNACK -> c.tileSand
    }
    val emoji = stringResource(
        when (meal.slot) {
            MealSlot.BREAKFAST -> R.string.bplan_emoji_breakfast
            MealSlot.LUNCH -> R.string.bplan_emoji_lunch
            MealSlot.DINNER -> R.string.bplan_emoji_dinner
            MealSlot.SNACK -> R.string.bplan_emoji_snack
        },
    )
    Row(
        Modifier.fillMaxWidth().clip(BentoTileShape).background(tile.container).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, lineHeight = 28.sp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                slotName(meal.slot).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
                color = tile.content.copy(alpha = 0.6f), maxLines = 1,
            )
            Text(
                meal.items.joinToString(" + ") { (if (it.servings > 1) "${it.servings} " else "") + it.food.name },
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 20.sp),
                color = tile.content, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.food_kcal, "%,d".format(meal.kcal)),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = tile.content,
            )
        }
        Spacer(Modifier.width(8.dp))
        val bg = if (done) c.tileInk.container else c.tileInk.content.copy(alpha = 0.7f)
        val fg = if (done) c.highlight else c.tileInk.container
        Box(
            Modifier.size(40.dp).clip(SmoothCornerShape(12.dp)).background(bg).clickable(enabled = !done, onClick = onAte),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (done) Icons.Rounded.Check else Icons.Rounded.Add,
                contentDescription = if (done) stringResource(R.string.food_logged, slotName(meal.slot)) else stringResource(R.string.plan_ate_this),
                tint = fg, modifier = Modifier.size(24.dp),
            )
        }
    }
}
