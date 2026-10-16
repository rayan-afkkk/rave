package com.rave.app.ui.food

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import coil.compose.AsyncImage
import com.rave.app.R
import com.rave.app.data.food.FoodLookupException
import com.rave.app.data.food.FoodRepository
import com.rave.app.data.food.OpenFoodFactsClient
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.food.Allergen
import com.rave.app.domain.food.FoodAssessment
import com.rave.app.domain.food.FoodSource
import com.rave.app.domain.food.FoodVerdictEngine
import com.rave.app.domain.food.FoodWarning
import com.rave.app.domain.food.MealSlot
import com.rave.app.domain.food.Nutrients
import com.rave.app.domain.food.ScannedFood
import com.rave.app.domain.food.WarningType
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.PillChip
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.TonalPillButton
import com.rave.app.ui.navigation.FoodResultRoute
import com.rave.app.ui.theme.Dimens
import com.rave.app.ui.theme.RaveTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

sealed interface ResultUi {
    data object Loading : ResultUi
    data class NotFound(val barcode: String) : ResultUi
    data object Offline : ResultUi
    data object LimitReached : ResultUi
    data class Ready(val food: ScannedFood, val assessment: FoodAssessment) : ResultUi
}

@HiltViewModel
class FoodResultViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val off: OpenFoodFactsClient,
    private val food: FoodRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val barcode = savedState.toRoute<FoodResultRoute>().barcode
    private val _ui = MutableStateFlow<ResultUi>(ResultUi.Loading)
    val ui: StateFlow<ResultUi> = _ui.asStateFlow()
    private val _logged = MutableStateFlow<MealSlot?>(null)
    val logged: StateFlow<MealSlot?> = _logged.asStateFlow()
    val defaultSlot = food.slotNow()

    init { load() }

    fun upgrade() = food.showScanPaywall(FoodSource.BARCODE)

    fun load() = viewModelScope.launch {
        _ui.value = ResultUi.Loading
        val scanned = if (barcode.isBlank()) food.pendingScan.value else {
            if (!food.canScan(FoodSource.BARCODE)) { _ui.value = ResultUi.LimitReached; food.showScanPaywall(FoodSource.BARCODE); return@launch }
            try { off.lookup(barcode) } catch (e: FoodLookupException) { _ui.value = ResultUi.Offline; return@launch }
        }
        if (scanned == null) {
            _ui.value = ResultUi.NotFound(barcode)
            return@launch
        }
        val assessment = FoodVerdictEngine.assess(scanned, settings.current().food)
        food.recordScan(scanned, assessment)
        _ui.value = ResultUi.Ready(scanned, assessment)
    }

    fun log(slot: MealSlot) {
        val r = _ui.value as? ResultUi.Ready ?: return
        viewModelScope.launch {
            food.logScanned(r.food, r.assessment, slot)
            _logged.value = slot
        }
    }
}

@Composable
fun FoodResultScreen(onBack: () -> Unit, onScanAnother: () -> Unit, viewModel: FoodResultViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val logged by viewModel.logged.collectAsStateWithLifecycle()
    val context = LocalContext.current
    RaveScreen(title = stringResource(R.string.food_result_title), onBack = onBack) {
        when (val u = ui) {
            ResultUi.Loading -> LoadingState()
            ResultUi.LimitReached -> Column(Modifier.padding(Dimens.ScreenPadding)) {
                EmptyState(Icons.Rounded.Error, stringResource(R.string.food_limit_reached), "")
                com.rave.app.ui.components.PillButton(stringResource(R.string.pro_unlock), onClick = viewModel::upgrade, modifier = Modifier.fillMaxWidth())
            }
            ResultUi.Offline -> Column(Modifier.padding(Dimens.ScreenPadding)) {
                EmptyState(Icons.Rounded.Error, stringResource(R.string.food_lookup_failed), "")
                TonalPillButton(stringResource(R.string.action_retry), onClick = { viewModel.load() }, modifier = Modifier.fillMaxWidth())
            }
            is ResultUi.NotFound -> Column(Modifier.padding(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EmptyState(Icons.Rounded.SearchOff, stringResource(R.string.food_not_found_title), stringResource(R.string.food_not_found_body))
                PillButton(stringResource(R.string.food_scan_another), onClick = onScanAnother, modifier = Modifier.fillMaxWidth())
                if (u.barcode.isNotBlank()) {
                    TextButton(onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://world.openfoodfacts.org/product/${u.barcode}"))) }
                    }, modifier = Modifier.fillMaxWidth()) { Text("openfoodfacts.org") }
                }
            }
            is ResultUi.Ready -> ResultContent(u.food, u.assessment, viewModel.defaultSlot, logged, viewModel::log, onScanAnother)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultContent(
    food: ScannedFood, a: FoodAssessment, defaultSlot: MealSlot, logged: MealSlot?,
    onLog: (MealSlot) -> Unit, onScanAnother: () -> Unit,
) {
    val c = verdictColors(a.verdict)
    var slot by remember { mutableStateOf(defaultSlot) }
    Column(
        Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            food.imageUrl?.let {
                AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.medium))
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(food.name, style = MaterialTheme.typography.titleLarge)
                val sub = listOf(food.brand, food.servingLabel).filter { it.isNotBlank() }.joinToString(" · ")
                if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // Verdict hero
        SectionCard(Modifier.fillMaxWidth(), color = c.container) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(c.content), contentAlignment = Alignment.Center) {
                    Icon(verdictIcon(a.verdict), null, tint = c.container, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(verdictTitle(a.verdict), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = c.content)
                    Text(verdictBody(a.verdict), style = MaterialTheme.typography.bodyMedium, color = c.content)
                }
            }
            a.sugarOfDailyLimit?.let { LimitBar(stringResource(R.string.food_sugar_limit, (it * 100).roundToInt()), it, c.content) }
            a.saltOfDailyLimit?.let { LimitBar(stringResource(R.string.food_salt_limit, (it * 100).roundToInt()), it, c.content) }
        }
        if (a.reasons.isNotEmpty()) {
            SectionCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.food_why), style = MaterialTheme.typography.titleSmall)
                a.reasons.forEach { r ->
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val good = r.type.positive
                        Icon(if (good) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber, null,
                            tint = if (good) RaveTheme.colors.verdictGo.content else RaveTheme.colors.verdictSkip.content, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(reasonText(r), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (a.warnings.isNotEmpty()) {
            SectionCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.food_warnings), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    a.warnings.forEach { w -> WarningChip(w) }
                }
            }
        }
        if (a.swaps.isNotEmpty()) {
            SectionCard(Modifier.fillMaxWidth(), color = RaveTheme.colors.verdictGo.container) {
                Text(stringResource(R.string.food_try_instead), style = MaterialTheme.typography.titleSmall, color = RaveTheme.colors.verdictGo.content)
                a.swaps.forEach { s -> Text("• " + swapText(s), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp)) }
            }
        }
        NutritionTable(food)
        // Log
        SectionCard(Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER, MealSlot.SNACK).forEach { s -> PillChip(slot == s, slotName(s), { slot = s }) }
            }
            Spacer(Modifier.height(10.dp))
            if (logged != null) {
                Text(stringResource(R.string.food_logged, slotName(logged)), style = MaterialTheme.typography.titleSmall, color = RaveTheme.colors.verdictGo.content)
            } else {
                val kcal = food.portion?.kcal?.roundToInt()
                PillButton(
                    stringResource(R.string.food_log_action) + (kcal?.let { " · " + stringResource(R.string.food_kcal, it.toString()) } ?: ""),
                    onClick = { onLog(slot) }, modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(6.dp))
            TonalPillButton(stringResource(R.string.food_scan_another), onClick = onScanAnother, modifier = Modifier.fillMaxWidth())
        }
        Text(stringResource(R.string.food_disclaimer), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LimitBar(label: String, fraction: Double, color: androidx.compose.ui.graphics.Color) {
    Column(Modifier.padding(top = 10.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        Box(Modifier.fillMaxWidth().padding(top = 3.dp).height(6.dp).clip(CircleShape).background(color.copy(alpha = 0.2f))) {
            Box(Modifier.fillMaxWidth(fraction.toFloat().coerceIn(0.02f, 1f)).height(6.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun WarningChip(w: FoodWarning) {
    val text = when (w.type) {
        WarningType.ALLERGENS -> stringResource(R.string.food_warn_allergens,
            w.items.mapNotNull { runCatching { Allergen.valueOf(it) }.getOrNull() }.map { allergenName(it) }.joinToString())
        WarningType.HALAL_CHECK -> stringResource(R.string.food_warn_halal_check, w.items.joinToString())
        WarningType.HALAL_CERTIFIED -> stringResource(R.string.food_warn_halal_ok)
        WarningType.NOT_VEGETARIAN -> stringResource(R.string.food_warn_not_veg, w.items.joinToString())
    }
    val tile = if (w.type == WarningType.HALAL_CERTIFIED) RaveTheme.colors.verdictGo else RaveTheme.colors.verdictSometimes
    Box(Modifier.clip(MaterialTheme.shapes.small).background(tile.container).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = tile.content)
    }
}

@Composable
private fun NutritionTable(food: ScannedFood) {
    val per100 = food.per100g
    val portion = food.perServing ?: food.portion?.takeIf { food.servingGrams != null }
    if (per100 == null && portion == null) return
    SectionCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.food_nutrition), style = MaterialTheme.typography.titleSmall)
        Row(Modifier.padding(top = 6.dp)) {
            Spacer(Modifier.weight(1.4f))
            if (per100 != null) Text(stringResource(R.string.food_per_100), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            if (portion != null) Text(stringResource(R.string.food_per_portion), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        }
        NutrientRow(per100, portion, R.string.food_energy, { it.kcal }, "kcal")
        NutrientRow(per100, portion, R.string.food_protein, { it.protein }, "g")
        NutrientRow(per100, portion, R.string.food_carbs, { it.carbs }, "g")
        NutrientRow(per100, portion, R.string.food_sugar, { it.sugar }, "g")
        NutrientRow(per100, portion, R.string.food_fat, { it.fat }, "g")
        NutrientRow(per100, portion, R.string.food_sat_fat, { it.satFat }, "g")
        NutrientRow(per100, portion, R.string.food_fiber, { it.fiber }, "g")
        NutrientRow(per100, portion, R.string.food_salt, { it.salt }, "g")
    }
}

@Composable
private fun NutrientRow(per100: Nutrients?, portion: Nutrients?, label: Int, get: (Nutrients) -> Double?, unit: String) {
    val a = per100?.let(get)
    val b = portion?.let(get)
    if (a == null && b == null) return
    Row(Modifier.padding(top = 4.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.4f))
        if (per100 != null) Text(a?.let { "%.1f $unit".format(it) } ?: "–", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        if (portion != null) Text(b?.let { "%.1f $unit".format(it) } ?: "–", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}
