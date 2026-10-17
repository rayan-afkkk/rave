package com.rave.app.ui.food

import android.content.Intent
import com.rave.app.ui.components.SmoothCornerShape
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.ui.unit.sp
import com.rave.app.domain.food.ReasonType
import com.rave.app.ui.components.AppBackground
import com.rave.app.ui.components.BentoBig
import com.rave.app.ui.components.BentoButton
import com.rave.app.ui.components.BentoChip
import com.rave.app.ui.components.BentoGap
import com.rave.app.ui.components.BentoKicker
import com.rave.app.ui.components.BentoOutlinedCard
import com.rave.app.ui.components.BentoSquareShape
import com.rave.app.ui.components.BentoTile
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
import com.rave.app.ui.navigation.FoodResultRoute
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
    data class Ready(val food: ScannedFood, val assessment: FoodAssessment, val halalOnly: Boolean = true) : ResultUi
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
        val prefs = settings.current().food
        val assessment = FoodVerdictEngine.assess(scanned, prefs)
        food.recordScan(scanned, assessment)
        _ui.value = ResultUi.Ready(scanned, assessment, prefs.halalOnly)
    }

    fun log(slot: MealSlot) {
        val r = _ui.value as? ResultUi.Ready ?: return
        viewModelScope.launch {
            food.logScanned(r.food, r.assessment, slot)
            _logged.value = slot
        }
    }
}

private val ResultGutter = 18.dp

@Composable
fun FoodResultScreen(onBack: () -> Unit, onScanAnother: () -> Unit, viewModel: FoodResultViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val logged by viewModel.logged.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val c = RaveTheme.colors
    val ready = ui as? ResultUi.Ready
    val shareText = ready?.let {
        stringResource(R.string.bscan_share_text, it.food.name, verdictHeadline(it.assessment.verdict), stringResource(R.string.app_name))
    }
    val chooserTitle = stringResource(R.string.share_chooser)
    AppBackground {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(Modifier.weight(1f))
                if (shareText != null) {
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                        runCatching { context.startActivity(Intent.createChooser(send, chooserTitle)) }
                    }) {
                        Icon(Icons.Rounded.IosShare, stringResource(R.string.action_share), tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
            when (val u = ui) {
                ResultUi.Loading -> LoadingState()
                ResultUi.LimitReached -> Column(Modifier.padding(ResultGutter), verticalArrangement = Arrangement.spacedBy(BentoGap)) {
                    EmptyState(Icons.Rounded.Error, stringResource(R.string.food_limit_reached), "")
                    BentoButton(stringResource(R.string.pro_unlock), c.tileInk.container, c.tileInk.content, onClick = viewModel::upgrade,
                        modifier = Modifier.fillMaxWidth())
                }
                ResultUi.Offline -> Column(Modifier.padding(ResultGutter), verticalArrangement = Arrangement.spacedBy(BentoGap)) {
                    EmptyState(Icons.Rounded.Error, stringResource(R.string.food_lookup_failed), "")
                    BentoButton(stringResource(R.string.action_retry), c.tileSand.container, c.tileSand.content, onClick = { viewModel.load() },
                        modifier = Modifier.fillMaxWidth())
                }
                is ResultUi.NotFound -> Column(Modifier.padding(ResultGutter), verticalArrangement = Arrangement.spacedBy(BentoGap)) {
                    EmptyState(Icons.Rounded.SearchOff, stringResource(R.string.food_not_found_title), stringResource(R.string.food_not_found_body))
                    BentoButton(stringResource(R.string.food_scan_another), c.tileInk.container, c.tileInk.content, onClick = onScanAnother,
                        modifier = Modifier.fillMaxWidth())
                    if (u.barcode.isNotBlank()) {
                        TextButton(onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://world.openfoodfacts.org/product/${u.barcode}"))) }
                        }, modifier = Modifier.fillMaxWidth()) { Text("openfoodfacts.org") }
                    }
                }
                is ResultUi.Ready -> ResultContent(u.food, u.assessment, u.halalOnly, viewModel.defaultSlot, logged, viewModel::log, onScanAnother)
            }
        }
    }
}

private fun grams(v: Double) = if (v >= 10) "%.0f".format(v) else "%.1f".format(v)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultContent(
    food: ScannedFood, a: FoodAssessment, halalOnly: Boolean, defaultSlot: MealSlot, logged: MealSlot?,
    onLog: (MealSlot) -> Unit, onScanAnother: () -> Unit,
) {
    val c = RaveTheme.colors
    val portion = food.portion
    Column(
        Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = ResultGutter).padding(top = 6.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(BentoGap),
    ) {
        VerdictHero(food, a)
        // SUGAR (black) / ENERGY (sand)
        val sugar = portion?.sugar
        val kcal = portion?.kcal
        if (sugar != null || kcal != null) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(BentoGap)) {
                if (sugar != null) {
                    BentoTile(c.tileInk.container, Modifier.weight(1f).fillMaxHeight()) {
                        BentoKicker(stringResource(R.string.food_sugar), c.tileInk.content)
                        BentoBig(stringResource(R.string.bscan_grams, grams(sugar)), c.tileInk.content, Modifier.padding(top = 6.dp), size = 32.sp, maxLines = 1)
                    }
                }
                if (kcal != null) {
                    BentoTile(c.tileSand.container, Modifier.weight(1f).fillMaxHeight()) {
                        BentoKicker(stringResource(R.string.food_energy), c.tileSand.content)
                        BentoBig(kcal.roundToInt().toString(), c.tileSand.content, Modifier.padding(top = 6.dp), size = 32.sp, maxLines = 1)
                        Text(stringResource(R.string.unit_kcal), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = c.tileSand.content.copy(alpha = 0.65f))
                    }
                }
            }
        }
        HalalTile(food, a, halalOnly)
        if (a.swaps.isNotEmpty()) {
            val t = c.tileBlue
            BentoTile(t.container, Modifier.fillMaxWidth()) {
                BentoKicker(stringResource(R.string.food_try_instead), t.content)
                FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // White pill chips with ink text.
                    a.swaps.forEach { s -> BentoChip(swapEmoji(s) + " " + swapText(s), container = c.tileInk.content, content = c.tileInk.container) }
                }
            }
        }
        LogRow(defaultSlot, logged, onLog, onScanAnother, Modifier.padding(top = 2.dp))
        if (a.reasons.isNotEmpty()) {
            val t = c.tilePink
            BentoTile(t.container, Modifier.fillMaxWidth()) {
                BentoKicker(stringResource(R.string.food_why), t.content)
                a.reasons.forEach { r ->
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val good = r.type.positive
                        Icon(if (good) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber, null, tint = t.content, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(reasonText(r), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = t.content)
                    }
                }
            }
        }
        val otherWarnings = a.warnings.filter { it.type == WarningType.ALLERGENS || it.type == WarningType.NOT_VEGETARIAN }
        if (otherWarnings.isNotEmpty()) {
            val t = c.tilePeach
            BentoTile(t.container, Modifier.fillMaxWidth()) {
                BentoKicker(stringResource(R.string.food_warnings), t.content)
                FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    otherWarnings.forEach { w -> BentoChip(warningText(w), container = c.tileInk.container, content = c.tileInk.content) }
                }
            }
        }
        NutritionTable(food)
        Text(stringResource(R.string.food_disclaimer), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp))
    }
}

/** Big verdict tile: kicker "COLA · 500 ML", huge "Skip it.", subline, black icon square and data chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VerdictHero(food: ScannedFood, a: FoodAssessment) {
    val t = verdictHeroColors(a.verdict)
    val ink = RaveTheme.colors.tileInk
    BentoTile(t.container, Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BentoKicker(listOf(food.name, food.servingLabel).filter { it.isNotBlank() }.joinToString(" · "), t.content)
                BentoBig(verdictHeadline(a.verdict), t.content, Modifier.padding(top = 8.dp), size = 38.sp, maxLines = 1)
                Text(verdictSub(a.verdict), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = t.content,
                    modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(72.dp).clip(SmoothCornerShape(22.dp)).background(ink.container), contentAlignment = Alignment.Center) {
                Icon(verdictIcon(a.verdict), verdictTitle(a.verdict), tint = t.container, modifier = Modifier.size(40.dp))
            }
        }
        val soft = t.content.copy(alpha = 0.12f)
        val chips = buildList<Pair<String, Boolean>> {
            a.sugarOfDailyLimit?.let { add(stringResource(R.string.bscan_daily_sugar, (it * 100).roundToInt()) to true) }
            a.saltOfDailyLimit?.let { add(stringResource(R.string.bscan_daily_salt, (it * 100).roundToInt()) to false) }
            food.novaGroup?.let { add(stringResource(R.string.bscan_nova, it) to false) }
            food.nutriScore?.let { add(stringResource(R.string.bscan_nutri, it.uppercaseChar().toString()) to false) }
        }
        if (chips.isNotEmpty()) {
            FlowRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                chips.forEach { (text, strong) ->
                    if (strong) BentoChip(text, container = ink.container, content = ink.content)
                    else BentoChip(text, container = soft, content = t.content)
                }
            }
        }
    }
}

/** HALAL CHECK tile: lime when clear/certified, yellow when doubtful, coral when not halal. Hidden when there is nothing to say. */
@Composable
private fun HalalTile(food: ScannedFood, a: FoodAssessment, halalOnly: Boolean) {
    val c = RaveTheme.colors
    val notHalal = a.reasons.firstOrNull { it.type == ReasonType.NOT_HALAL }
    val doubtful = a.warnings.firstOrNull { it.type == WarningType.HALAL_CHECK }
    val certified = a.warnings.any { it.type == WarningType.HALAL_CERTIFIED }
    val (tile, title) = when {
        notHalal != null -> c.verdictHeroSkip to stringResource(R.string.bscan_halal_not, notHalal.text.orEmpty())
        doubtful != null -> c.verdictHeroSometimes to stringResource(R.string.bscan_halal_doubtful, doubtful.items.joinToString())
        certified -> c.verdictHeroGo to stringResource(R.string.bscan_halal_certified)
        halalOnly && food.ingredients.isNotBlank() -> c.verdictHeroGo to stringResource(R.string.bscan_halal_clear)
        else -> return
    }
    val additives = food.additiveTags.map { it.substringAfter(':').uppercase() }.filter { it.isNotBlank() }.take(4)
    BentoTile(tile.container, Modifier.fillMaxWidth()) {
        BentoKicker(stringResource(R.string.bscan_halal_check), tile.content)
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold), color = tile.content,
            modifier = Modifier.padding(top = 6.dp))
        if (additives.isNotEmpty()) {
            Text(stringResource(R.string.bscan_contains, additives.joinToString(" · ")), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = tile.content.copy(alpha = 0.65f))
        }
    }
}

/** "Scan another" (sand) / "Log it +" (black). Log it opens a meal-slot menu. */
@Composable
private fun LogRow(defaultSlot: MealSlot, logged: MealSlot?, onLog: (MealSlot) -> Unit, onScanAnother: () -> Unit, modifier: Modifier = Modifier) {
    val c = RaveTheme.colors
    var menu by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BentoGap)) {
        BentoButton(stringResource(R.string.food_scan_another), c.tileSand.container, c.tileSand.content, onClick = onScanAnother,
            modifier = Modifier.weight(1f), height = 50.dp)
        Box(Modifier.weight(1f)) {
            if (logged != null) {
                BentoButton(stringResource(R.string.food_logged, slotName(logged)), c.highlight, c.onHighlight, onClick = { menu = true },
                    modifier = Modifier.fillMaxWidth(), leading = Icons.Rounded.Check, height = 50.dp)
            } else {
                BentoButton(stringResource(R.string.bscan_log_it), c.tileInk.container, c.tileInk.content, onClick = { menu = true },
                    modifier = Modifier.fillMaxWidth(), trailing = Icons.Rounded.Add, height = 50.dp)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER, MealSlot.SNACK).forEach { s ->
                    DropdownMenuItem(
                        text = { Text(slotName(s), fontWeight = if (s == (logged ?: defaultSlot)) FontWeight.ExtraBold else FontWeight.Normal) },
                        onClick = { menu = false; onLog(s) },
                    )
                }
            }
        }
    }
}

@Composable
private fun warningText(w: FoodWarning): String = when (w.type) {
    WarningType.ALLERGENS -> stringResource(R.string.food_warn_allergens,
        w.items.mapNotNull { runCatching { Allergen.valueOf(it) }.getOrNull() }.map { allergenName(it) }.joinToString())
    WarningType.HALAL_CHECK -> stringResource(R.string.food_warn_halal_check, w.items.joinToString())
    WarningType.HALAL_CERTIFIED -> stringResource(R.string.food_warn_halal_ok)
    WarningType.NOT_VEGETARIAN -> stringResource(R.string.food_warn_not_veg, w.items.joinToString())
}

@Composable
private fun NutritionTable(food: ScannedFood) {
    val per100 = food.per100g
    val portion = food.perServing ?: food.portion?.takeIf { food.servingGrams != null }
    if (per100 == null && portion == null) return
    BentoOutlinedCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BentoKicker(stringResource(R.string.food_nutrition), MaterialTheme.colorScheme.onSurface)
                if (food.brand.isNotBlank()) {
                    Text(food.brand, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), modifier = Modifier.padding(top = 4.dp))
                }
            }
            food.imageUrl?.let {
                AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp).clip(BentoSquareShape))
            }
        }
        Row(Modifier.padding(top = 8.dp)) {
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
