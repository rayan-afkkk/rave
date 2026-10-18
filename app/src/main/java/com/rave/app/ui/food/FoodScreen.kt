package com.rave.app.ui.food

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import com.rave.app.ui.components.BentoBig
import com.rave.app.ui.components.BentoButton
import com.rave.app.ui.components.BentoChip
import com.rave.app.ui.components.BentoGap
import com.rave.app.ui.components.BentoIconButton
import com.rave.app.ui.components.BentoKicker
import com.rave.app.ui.components.BentoPillShape
import com.rave.app.ui.components.BentoSquareShape
import com.rave.app.ui.components.BentoTile
import com.rave.app.ui.components.BentoTileShape
import com.rave.app.ui.components.BentoTitle
import com.rave.app.ui.components.BentoToggleChip
import com.rave.app.ui.theme.TileColor
import com.rave.app.R
import com.rave.app.data.food.DayFood
import com.rave.app.data.food.FoodAi
import com.rave.app.data.food.FoodRepository
import com.rave.app.data.food.LabelReader
import com.rave.app.data.local.entity.FoodLogEntity
import com.rave.app.data.local.entity.FoodScanEntity
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.StepRepository
import com.rave.app.domain.fitness.FitnessMath
import com.rave.app.domain.food.DailyTarget
import com.rave.app.domain.food.Cuisine
import com.rave.app.domain.food.FoodCatalog
import com.rave.app.domain.food.FoodItem
import com.rave.app.domain.food.FoodSource
import com.rave.app.domain.food.MealSlot
import com.rave.app.domain.food.Verdict
import com.rave.app.ui.permissions.hasPermission
import com.rave.app.ui.theme.RaveTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import javax.inject.Inject

data class FoodUiState(
    val day: DayFood = DayFood(emptyList()),
    val target: DailyTarget? = null,
    val burnedKcal: Int = 0,
    val scans: List<FoodScanEntity> = emptyList(),
    val cuisines: Set<Cuisine> = emptySet(),
    val halalOnly: Boolean = false,
)

@HiltViewModel
class FoodViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val food: FoodRepository,
    private val labels: LabelReader,
    private val ai: FoodAi,
    steps: StepRepository,
    settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<FoodUiState> = combine(
        food.observeToday(), food.observeTarget(), steps.observeToday(), settings.settings, food.observeRecentScans(),
    ) { day, target, stepsToday, s, scans ->
        FoodUiState(day, target, FitnessMath.stepCalories(stepsToday, s.weightKg, s.heightCm), scans, s.food.cuisines, s.food.halalOnly)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoodUiState())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _message = MutableStateFlow<Int?>(null)
    val message: StateFlow<Int?> = _message.asStateFlow()

    val photoScanAvailable get() = ai.isAvailable

    fun dismissMessage() { _message.value = null }

    fun newPhotoUri(): Uri {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Label photo → on-device OCR (AI reader as a fallback for hard photos) → result screen. */
    fun readLabel(uri: Uri, labelName: String, onReady: () -> Unit) = runScan(FoodSource.LABEL, onReady) {
        val ocr = runCatching { labels.read(uri, labelName) }.getOrNull()
        val result = ocr ?: if (ai.isAvailable) {
            withContext(Dispatchers.IO) { loadScaled(uri, 1600) }?.let { bmp -> runCatching { ai.readLabel(bmp, labelName) }.getOrNull() }
        } else {
            null
        }
        if (result == null) _message.value = R.string.food_label_failed
        result
    }

    private fun runScan(source: FoodSource, onReady: () -> Unit, block: suspend () -> com.rave.app.domain.food.ScannedFood?) {
        viewModelScope.launch {
            if (!food.canScan(source)) { _message.value = R.string.food_limit_reached; food.showScanPaywall(source); return@launch }
            _busy.value = true
            val result = runCatching { block() }.getOrElse { _message.value = R.string.food_label_failed; null }
            _busy.value = false
            if (result != null) {
                food.pendingScan.value = result
                onReady()
            }
        }
    }

    fun quickAdd(item: FoodItem, servings: Int, slot: MealSlot) = viewModelScope.launch {
        food.log(item.name, item.kcal * servings, item.protein * servings, item.carbs * servings, item.fat * servings, null, FoodSource.MANUAL, slot)
    }

    fun delete(id: Long) = viewModelScope.launch { food.delete(id) }

    fun slotNow() = food.slotNow()

    private fun loadScaled(uri: Uri, max: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > max * 2 || bounds.outHeight / sample > max * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val scale = max.toFloat() / maxOf(decoded.width, decoded.height)
        return if (scale >= 1f) decoded else Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
    }
}

private enum class PendingCapture { LABEL }

private val ScanGutter = 18.dp
private const val RECENT_CHIPS = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(
    contentPadding: PaddingValues,
    onScanBarcode: () -> Unit,
    onOpenResult: (barcode: String) -> Unit,
    onOpenPlan: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: FoodViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val labelName = stringResource(R.string.food_scanned_label)
    var pending by remember { mutableStateOf<PendingCapture?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var showAllScans by rememberSaveable { mutableStateOf(false) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = photoUri
        if (ok && uri != null) when (pending) {
            PendingCapture.LABEL -> viewModel.readLabel(uri, labelName) { onOpenResult("") }
            null -> Unit
        }
    }
    fun capture(kind: PendingCapture) {
        pending = kind
        photoUri = viewModel.newPhotoUri().also { takePicture.launch(it) }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pending?.let { capture(it) }
    }
    fun start(kind: PendingCapture) {
        pending = kind
        if (context.hasPermission(Manifest.permission.CAMERA)) capture(kind) else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    val c = RaveTheme.colors
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 12.dp,
                start = ScanGutter, end = ScanGutter,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(BentoGap),
        ) {
            item(key = "header") {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    BentoTitle(stringResource(R.string.bscan_title), Modifier.weight(1f))
                    BentoIconButton(Icons.Rounded.Tune, stringResource(R.string.food_settings), onOpenSettings, size = 48.dp)
                }
            }
            item(key = "barcode") { BarcodeHero(onScanBarcode) }
            item(key = "capture") {
                ScanTile(Icons.Rounded.DocumentScanner, stringResource(R.string.bscan_read_label), stringResource(R.string.food_scan_label_sub),
                    c.tilePink, Modifier.fillMaxWidth()) { start(PendingCapture.LABEL) }
            }
            message?.let { m ->
                item(key = "message") { MessageTile(stringResource(m), viewModel::dismissMessage) }
            }
            item(key = "today") { TodayTile(state, onAdd = { showAdd = true }) }
            if (state.scans.isNotEmpty()) {
                item(key = "recent") {
                    RecentScans(
                        scans = state.scans,
                        showAll = showAllScans,
                        onToggleAll = { showAllScans = !showAllScans },
                        onOpen = { scan -> scan.barcode?.let(onOpenResult) },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (state.day.entries.isNotEmpty()) {
                val bySlot = state.day.entries.groupBy { runCatching { MealSlot.valueOf(it.slot) }.getOrDefault(MealSlot.SNACK) }
                MealSlot.entries.filter { it in bySlot }.forEach { slot ->
                    item(key = "slot_$slot") { LogGroup(slot, bySlot.getValue(slot), viewModel::delete) }
                }
            }
            item(key = "plan") { MealPlanTile(onOpenPlan) }
            item(key = "disclaimer") {
                Text(stringResource(R.string.food_disclaimer), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        if (busy) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                BentoTile(c.tileInk.container) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = c.highlight, strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.food_reading), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = c.tileInk.content)
                    }
                }
            }
        }
    }

    if (showAdd) {
        ModalBottomSheet(onDismissRequest = { showAdd = false }, containerColor = MaterialTheme.colorScheme.surface) {
            QuickAddSheet(defaultSlot = viewModel.slotNow(), cuisines = state.cuisines, halalOnly = state.halalOnly) { food, servings, slot ->
                viewModel.quickAdd(food, servings, slot)
                showAdd = false
            }
        }
    }
}

private fun fmt(n: Int) = NumberFormat.getIntegerInstance().format(n)

/** Black BARCODE hero: headline, lime dashed frame with a barcode icon, lime "Open camera →" chip. */
@Composable
private fun BarcodeHero(onClick: () -> Unit) {
    val ink = RaveTheme.colors.tileInk
    val lime = RaveTheme.colors.highlight
    Box(Modifier.fillMaxWidth().height(250.dp).clip(BentoTileShape).background(ink.container).clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            BentoKicker(stringResource(R.string.bscan_barcode), ink.content)
            Text(
                stringResource(R.string.bscan_hero_title), color = ink.content, modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 28.6.sp, letterSpacing = (-0.5).sp),
            )
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 18.dp).size(width = 150.dp, height = 110.dp)
                .drawBehind {
                    val stroke = 3.dp.toPx()
                    drawRoundRect(
                        color = lime,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(18.dp.toPx() - stroke / 2),
                        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 6.dp.toPx()), 0f)),
                    )
                },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.QrCodeScanner, null, tint = lime, modifier = Modifier.size(54.dp)) }
        BentoChip(
            stringResource(R.string.food_open_camera), container = lime, content = RaveTheme.colors.onHighlight,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 18.dp), onClick = onClick,
        )
    }
}

/** Read label (lilac): icon, title, subtitle and an arrow. */
@Composable
private fun ScanTile(icon: ImageVector, label: String, sub: String, tile: TileColor, modifier: Modifier, onClick: () -> Unit) {
    BentoTile(tile.container, modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tile.content, modifier = Modifier.size(32.dp))
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold), color = tile.content,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(sub, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = tile.content.copy(alpha = 0.65f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = tile.content, modifier = Modifier.size(22.dp))
        }
    }
}

/** Error / free-scan-limit note; tap to dismiss. */
@Composable
private fun MessageTile(text: String, onDismiss: () -> Unit) {
    val t = RaveTheme.colors.verdictHeroSometimes
    BentoTile(t.container, Modifier.fillMaxWidth(), onClick = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = t.content, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.Close, stringResource(R.string.action_dismiss), tint = t.content, modifier = Modifier.size(20.dp))
        }
    }
}

/** Lime TODAY tile: "1,240 / 2,050 kcal", black "+ Log food" chip and a black progress bar. */
@Composable
private fun TodayTile(state: FoodUiState, onAdd: () -> Unit) {
    val lime = RaveTheme.colors.highlight
    val ink = RaveTheme.colors.onHighlight
    val target = state.target
    val eaten = state.day.kcal
    val suffix = if (target != null) stringResource(R.string.bscan_of_target, fmt(target.kcal)) else stringResource(R.string.bscan_kcal_suffix)
    BentoTile(lime, Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BentoKicker(stringResource(R.string.food_today), ink)
                Text(
                    buildAnnotatedString {
                        append(fmt(eaten))
                        withStyle(SpanStyle(fontSize = 16.sp, letterSpacing = (-0.5).sp)) { append(suffix) }
                    },
                    color = ink, maxLines = 1, modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp, lineHeight = 28.sp),
                )
            }
            BentoChip(
                stringResource(R.string.bscan_log_food), container = RaveTheme.colors.tileInk.container, content = RaveTheme.colors.tileInk.content,
                onClick = onAdd,
            )
        }
        val f = if (target != null && target.kcal > 0) (eaten / target.kcal.toFloat()).coerceIn(0f, 1f) else 0f
        Box(Modifier.fillMaxWidth().padding(top = 12.dp).height(10.dp).clip(CircleShape).background(ink.copy(alpha = 0.12f))) {
            if (f > 0f) Box(Modifier.fillMaxWidth(f).height(10.dp).clip(CircleShape).background(ink))
        }
    }
}

/** "Recent  ·  See all" and a row of small verdict-coloured chips (name + verdict word). */
@Composable
private fun RecentScans(
    scans: List<FoodScanEntity>,
    showAll: Boolean,
    onToggleAll: () -> Unit,
    onOpen: (FoodScanEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.bscan_recent), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            if (scans.size > RECENT_CHIPS) {
                Text(
                    stringResource(if (showAll) R.string.bscan_show_less else R.string.bscan_see_all),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    modifier = Modifier.clip(BentoPillShape).clickable(onClick = onToggleAll).padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
        val shown = if (showAll) scans else scans.take(RECENT_CHIPS)
        shown.chunked(RECENT_CHIPS).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { scan -> RecentChip(scan, Modifier.weight(1f)) { onOpen(scan) } }
                repeat(RECENT_CHIPS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun RecentChip(scan: FoodScanEntity, modifier: Modifier, onClick: () -> Unit) {
    val v = runCatching { Verdict.valueOf(scan.verdict) }.getOrDefault(Verdict.SOMETIMES)
    val t = verdictChipColors(v)
    Column(
        modifier.clip(BentoTileShape).background(t.container)
            .then(if (scan.barcode != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(scan.name, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = t.content,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        val sub = scan.kcal?.let { "${verdictShort(v)} · ${stringResource(R.string.food_kcal, fmt(it))}" } ?: verdictShort(v)
        Text(sub, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = t.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** One meal slot of today's log: sand tile with the slot name, total and each entry. */
@Composable
private fun LogGroup(slot: MealSlot, entries: List<FoodLogEntity>, onDelete: (Long) -> Unit) {
    val t = RaveTheme.colors.tileSand
    BentoTile(t.container, Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BentoKicker(slotName(slot), t.content, Modifier.weight(1f))
            Text(stringResource(R.string.food_kcal, fmt(entries.sumOf { it.kcal })), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = t.content)
        }
        entries.forEach { e ->
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                e.verdict?.let { v -> runCatching { Verdict.valueOf(v) }.getOrNull() }?.let { v ->
                    Box(Modifier.size(10.dp).clip(CircleShape).background(verdictHeroColors(v).container))
                    Spacer(Modifier.width(8.dp))
                }
                Text(e.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = t.content,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.food_kcal, fmt(e.kcal)), style = MaterialTheme.typography.labelMedium,
                    color = t.content.copy(alpha = 0.65f))
                IconButton(onClick = { onDelete(e.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.action_delete), tint = t.content, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** Peach MEAL PLAN tile that opens the week's plan. */
@Composable
private fun MealPlanTile(onClick: () -> Unit) {
    val t = RaveTheme.colors.tilePeach
    BentoTile(t.container, Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BentoKicker(stringResource(R.string.bscan_meal_plan_kicker), t.content)
                Text(stringResource(R.string.food_meal_plan_body), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = t.content, modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(48.dp).clip(CircleShape).background(RaveTheme.colors.tileInk.container), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.RestaurantMenu, stringResource(R.string.food_meal_plan), tint = RaveTheme.colors.tileInk.content, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun QuickAddSheet(defaultSlot: MealSlot, cuisines: Set<Cuisine>, halalOnly: Boolean, onAdd: (FoodItem, Int, MealSlot) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var picked by remember { mutableStateOf<FoodItem?>(null) }
    var servings by remember { mutableIntStateOf(1) }
    var slot by remember { mutableStateOf(defaultSlot) }
    val c = RaveTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = ScanGutter).padding(bottom = 24.dp)) {
        val p = picked
        if (p == null) {
            OutlinedTextField(query, { query = it }, placeholder = { Text(stringResource(R.string.food_search_hint)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = BentoSquareShape)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxWidth().height(380.dp)) {
                items(FoodCatalog.search(query, cuisines, halalOnly), key = { it.id }) { f ->
                    Row(Modifier.fillMaxWidth().clickable { picked = f; servings = 1 }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(f.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(f.serving, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(stringResource(R.string.food_kcal, f.kcal.toString()), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        } else {
            BentoBig(p.name, MaterialTheme.colorScheme.onSurface, size = 26.sp)
            Text(p.serving, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.food_servings), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                BentoIconButton(Icons.Rounded.Remove, null, onClick = { if (servings > 1) servings-- }, size = 40.dp)
                Text("$servings", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), modifier = Modifier.padding(horizontal = 14.dp))
                BentoIconButton(Icons.Rounded.Add, null, onClick = { if (servings < 9) servings++ }, size = 40.dp)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER, MealSlot.SNACK).forEach { s ->
                    BentoToggleChip(slotName(s), slot == s, { slot = s })
                }
            }
            Spacer(Modifier.height(14.dp))
            BentoButton(
                stringResource(R.string.food_log_action) + " · " + stringResource(R.string.food_kcal, (p.kcal * servings).toString()),
                container = c.tileInk.container, content = c.tileInk.content,
                onClick = { onAdd(p, servings, slot) }, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
