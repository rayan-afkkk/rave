package com.rave.app.ui.food

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
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
import com.rave.app.domain.food.DesiFood
import com.rave.app.domain.food.DesiFoods
import com.rave.app.domain.food.FoodSource
import com.rave.app.domain.food.MealSlot
import com.rave.app.domain.food.Verdict
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.TonalPillButton
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.permissions.hasPermission
import com.rave.app.ui.theme.Dimens
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
        FoodUiState(day, target, FitnessMath.stepCalories(stepsToday, s.weightKg, s.heightCm), scans)
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

    /** Label photo → on-device OCR → result screen. */
    fun readLabel(uri: Uri, labelName: String, onReady: () -> Unit) = runScan(FoodSource.LABEL, onReady) {
        labels.read(uri, labelName) ?: run { _message.value = R.string.food_label_failed; null }
    }

    /** Meal photo → Gemini → result screen. */
    fun analyzePhoto(uri: Uri, onReady: () -> Unit) = runScan(FoodSource.PHOTO, onReady) {
        if (!ai.isAvailable) { _message.value = R.string.food_photo_unavailable; return@runScan null }
        val bitmap = withContext(Dispatchers.IO) { loadScaled(uri, 1024) } ?: return@runScan null
        runCatching { ai.analyze(bitmap) }.getOrElse { _message.value = R.string.food_photo_unavailable; return@runScan null }
            ?: run { _message.value = R.string.food_photo_failed; null }
    }

    private fun runScan(source: FoodSource, onReady: () -> Unit, block: suspend () -> com.rave.app.domain.food.ScannedFood?) {
        viewModelScope.launch {
            if (!food.canScan(source)) { _message.value = R.string.food_limit_reached; return@launch }
            _busy.value = true
            val result = runCatching { block() }.getOrNull()
            _busy.value = false
            if (result != null) {
                food.pendingScan.value = result
                onReady()
            }
        }
    }

    fun quickAdd(item: DesiFood, servings: Int, slot: MealSlot) = viewModelScope.launch {
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

private enum class PendingCapture { LABEL, PHOTO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(
    onBack: () -> Unit,
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

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = photoUri
        if (ok && uri != null) when (pending) {
            PendingCapture.LABEL -> viewModel.readLabel(uri, labelName) { onOpenResult("") }
            PendingCapture.PHOTO -> viewModel.analyzePhoto(uri) { onOpenResult("") }
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

    RaveScreen(
        title = stringResource(R.string.food_title),
        onBack = onBack,
        actions = { IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, stringResource(R.string.food_settings)) } },
    ) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
            ) {
                item { CaloriesCard(state) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScanTile(Icons.Rounded.QrCodeScanner, stringResource(R.string.food_scan_barcode), Modifier.weight(1f), onScanBarcode)
                        ScanTile(Icons.Rounded.DocumentScanner, stringResource(R.string.food_scan_label), Modifier.weight(1f)) { start(PendingCapture.LABEL) }
                        ScanTile(Icons.Rounded.PhotoCamera, stringResource(R.string.food_snap_meal), Modifier.weight(1f)) { start(PendingCapture.PHOTO) }
                    }
                }
                message?.let { m ->
                    item {
                        SectionCard(Modifier.fillMaxWidth(), color = RaveTheme.colors.verdictSometimes.container, onClick = viewModel::dismissMessage) {
                            Text(stringResource(m), style = MaterialTheme.typography.bodyMedium, color = RaveTheme.colors.verdictSometimes.content)
                        }
                    }
                }
                item {
                    SectionCard(Modifier.fillMaxWidth(), onClick = onOpenPlan) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            com.rave.app.ui.components.IconTile(Icons.Rounded.RestaurantMenu, RaveTheme.colors.tileGreen)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.food_meal_plan), style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.food_meal_plan_body), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.food_today_log), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { showAdd = true }) {
                            Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.food_add))
                        }
                    }
                }
                if (state.day.entries.isEmpty()) {
                    item { Text(stringResource(R.string.food_empty_log), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    val bySlot = state.day.entries.groupBy { runCatching { MealSlot.valueOf(it.slot) }.getOrDefault(MealSlot.SNACK) }
                    MealSlot.entries.filter { it in bySlot }.forEach { slot ->
                        item(key = "slot_$slot") { LogGroup(slot, bySlot.getValue(slot), viewModel::delete) }
                    }
                }
                if (state.scans.isNotEmpty()) {
                    item { Text(stringResource(R.string.food_recent_scans), style = MaterialTheme.typography.titleMedium) }
                    items(state.scans, key = { "scan_${it.id}" }) { scan ->
                        ScanRow(scan) { scan.barcode?.let(onOpenResult) }
                    }
                }
                item {
                    Text(stringResource(R.string.food_disclaimer), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                }
            }
            if (busy) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.food_reading))
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        ModalBottomSheet(onDismissRequest = { showAdd = false }, containerColor = MaterialTheme.colorScheme.surface) {
            QuickAddSheet(defaultSlot = viewModel.slotNow()) { food, servings, slot ->
                viewModel.quickAdd(food, servings, slot)
                showAdd = false
            }
        }
    }
}

private fun fmt(n: Int) = NumberFormat.getIntegerInstance().format(n)

@Composable
fun CaloriesCard(
    state: FoodUiState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val target = state.target ?: return
    val eaten = state.day.kcal
    val progress by animateFloatAsState((eaten / target.kcal.toFloat()).coerceIn(0f, 1f), tween(800), label = "kcal")
    val over = eaten > target.kcal
    val accent = if (over) RaveTheme.colors.verdictSkip.content else MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    SectionCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(112.dp)) {
                    val stroke = 10.dp.toPx()
                    val inset = Offset(stroke / 2, stroke / 2)
                    val arc = Size(size.width - stroke, size.height - stroke)
                    drawArc(track, 0f, 360f, false, inset, arc, style = Stroke(stroke))
                    if (progress > 0f) drawArc(accent, -90f, 360f * progress, false, inset, arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(fmt(eaten), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text(stringResource(R.string.food_eaten), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.food_of_target, fmt(target.kcal)), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (over) stringResource(R.string.food_over, fmt(eaten - target.kcal)) else stringResource(R.string.food_left, fmt(target.kcal - eaten)),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = accent,
                )
                MacroBar(stringResource(R.string.food_protein), state.day.protein.toInt(), target.proteinG, RaveTheme.colors.tileGreen.content)
                MacroBar(stringResource(R.string.food_carbs), state.day.carbs.toInt(), target.carbsG, RaveTheme.colors.tileBlue.content)
                MacroBar(stringResource(R.string.food_fat), state.day.fat.toInt(), target.fatG, RaveTheme.colors.tilePink.content)
            }
        }
        if (state.burnedKcal > 0) {
            Text(stringResource(R.string.food_burned, fmt(state.burnedKcal)), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
        footer?.invoke(this)
    }
}

@Composable
private fun MacroBar(label: String, value: Int, target: Int, color: androidx.compose.ui.graphics.Color) {
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.food_macro, value, target), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.fillMaxWidth().padding(top = 2.dp).height(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
            val f = if (target <= 0) 0f else (value / target.toFloat()).coerceIn(0f, 1f)
            if (f > 0f) Box(Modifier.fillMaxWidth(f).height(5.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun ScanTile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    SectionCard(modifier, onClick = onClick) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun LogGroup(slot: MealSlot, entries: List<FoodLogEntity>, onDelete: (Long) -> Unit) {
    SectionCard(Modifier.fillMaxWidth()) {
        Row {
            Text(slotName(slot), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.food_kcal, fmt(entries.sumOf { it.kcal })), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
        }
        entries.forEach { e ->
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                e.verdict?.let { v -> runCatching { Verdict.valueOf(v) }.getOrNull() }?.let { v ->
                    Box(Modifier.size(10.dp).clip(CircleShape).background(verdictColors(v).content))
                    Spacer(Modifier.width(8.dp))
                }
                Text(e.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
                Text(stringResource(R.string.food_kcal, fmt(e.kcal)), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { onDelete(e.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.action_delete), Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ScanRow(scan: FoodScanEntity, onClick: () -> Unit) {
    val v = runCatching { Verdict.valueOf(scan.verdict) }.getOrDefault(Verdict.SOMETIMES)
    val c = verdictColors(v)
    SectionCard(Modifier.fillMaxWidth(), onClick = if (scan.barcode != null) onClick else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(c.container), contentAlignment = Alignment.Center) {
                Icon(verdictIcon(v), null, tint = c.content, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(scan.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(listOf(scan.brand, verdictTitle(v)).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            scan.kcal?.let { Text(stringResource(R.string.food_kcal, fmt(it)), style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun QuickAddSheet(defaultSlot: MealSlot, onAdd: (DesiFood, Int, MealSlot) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var picked by remember { mutableStateOf<DesiFood?>(null) }
    var servings by remember { mutableIntStateOf(1) }
    var slot by remember { mutableStateOf(defaultSlot) }
    Column(Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding).padding(bottom = 24.dp)) {
        val p = picked
        if (p == null) {
            OutlinedTextField(query, { query = it }, placeholder = { Text(stringResource(R.string.food_search_hint)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxWidth().height(380.dp)) {
                items(DesiFoods.search(query), key = { it.id }) { f ->
                    Row(Modifier.fillMaxWidth().clickable { picked = f; servings = 1 }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(f.name, style = MaterialTheme.typography.bodyMedium)
                            Text(f.serving, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(stringResource(R.string.food_kcal, f.kcal.toString()), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        } else {
            Text(p.name, style = MaterialTheme.typography.titleMedium)
            Text(p.serving, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.food_servings), modifier = Modifier.weight(1f))
                TextButton(onClick = { if (servings > 1) servings-- }) { Text("−") }
                Text("$servings", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { if (servings < 9) servings++ }) { Text("+") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER, MealSlot.SNACK).forEach { s ->
                    com.rave.app.ui.components.PillChip(slot == s, slotName(s), { slot = s })
                }
            }
            Spacer(Modifier.height(12.dp))
            TonalPillButton(
                stringResource(R.string.food_log_action) + " · " + stringResource(R.string.food_kcal, (p.kcal * servings).toString()),
                onClick = { onAdd(p, servings, slot) }, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
