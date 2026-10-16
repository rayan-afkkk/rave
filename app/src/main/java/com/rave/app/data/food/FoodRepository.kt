package com.rave.app.data.food

import com.rave.app.core.TimeProvider
import com.rave.app.data.local.dao.FoodDao
import com.rave.app.data.local.entity.FoodLogEntity
import com.rave.app.data.local.entity.FoodScanEntity
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.StepRepository
import com.rave.app.domain.food.CalorieTarget
import com.rave.app.domain.food.DailyTarget
import com.rave.app.domain.food.FoodAssessment
import com.rave.app.domain.food.FoodSource
import com.rave.app.domain.food.MealSlot
import com.rave.app.domain.food.ScannedFood
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

data class DayFood(val entries: List<FoodLogEntity>) {
    val kcal: Int get() = entries.sumOf { it.kcal }
    val protein: Double get() = entries.sumOf { it.protein }
    val carbs: Double get() = entries.sumOf { it.carbs }
    val fat: Double get() = entries.sumOf { it.fat }
}

/** Daily food-scan limits for free users; Rave Pro removes them. */
object FoodGate {
    const val FREE_BARCODE_SCANS_PER_DAY = 5
    /** AI scans: meal photos and label reads together. */
    const val FREE_AI_SCANS_PER_DAY = 2
}

@Singleton
class FoodRepository @Inject constructor(
    private val dao: FoodDao,
    private val settings: SettingsRepository,
    private val steps: StepRepository,
    private val time: TimeProvider,
    private val pro: com.rave.app.data.billing.ProRepository,
) {
    /** Result of the latest label/photo scan, handed to the result screen. */
    val pendingScan = MutableStateFlow<ScannedFood?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeToday(): Flow<DayFood> = time.todayFlow.flatMapLatest { d -> dao.observeDay(d.toString()).map { DayFood(it) } }

    fun observeRecentScans(): Flow<List<FoodScanEntity>> = dao.observeRecentScans()

    /** Daily calories and macros from body stats, goal and the last week's average steps. */
    fun observeTarget(): Flow<DailyTarget> = combine(settings.settings, steps.observeLastDays(7)) { s, week ->
        val avgSteps = week.map { it.second }.filter { it > 0 }.average().takeIf { !it.isNaN() }?.toInt() ?: 5_000
        val age = if (s.birthYear > 0) time.today().year - s.birthYear else 25
        CalorieTarget.target(s.weightKg, s.heightCm, age, s.sex, avgSteps, s.food.goal)
    }

    suspend fun log(name: String, kcal: Int, protein: Double, carbs: Double, fat: Double, sugar: Double?, source: FoodSource,
                    slot: MealSlot = slotNow(), verdict: String? = null, barcode: String? = null) {
        dao.insertLog(
            FoodLogEntity(
                date = time.today().toString(), slot = slot.name, name = name, kcal = kcal, protein = protein, carbs = carbs,
                fat = fat, sugar = sugar, source = source.name, verdict = verdict, barcode = barcode, createdAt = time.nowMs(),
            ),
        )
    }

    suspend fun logScanned(food: ScannedFood, assessment: FoodAssessment, slot: MealSlot = slotNow()) {
        val p = food.portion
        log(
            name = food.name, kcal = p?.kcal?.toInt() ?: 0, protein = p?.protein ?: 0.0, carbs = p?.carbs ?: 0.0, fat = p?.fat ?: 0.0,
            sugar = p?.sugar, source = food.source, slot = slot, verdict = assessment.verdict.name, barcode = food.barcode,
        )
    }

    suspend fun delete(id: Long) = dao.deleteLog(id)

    suspend fun recordScan(food: ScannedFood, assessment: FoodAssessment) {
        dao.insertScan(
            FoodScanEntity(
                barcode = food.barcode, name = food.name, brand = food.brand, verdict = assessment.verdict.name,
                kcal = food.portion?.kcal?.toInt(), imageUrl = food.imageUrl, source = food.source.name, scannedAt = time.nowMs(),
            ),
        )
    }

    /** True if a free user may scan again today with [source]. */
    suspend fun canScan(source: FoodSource): Boolean {
        if (pro.isPro.value) return true
        val since = time.startOfDayMs(time.today())
        return if (source == FoodSource.BARCODE) {
            dao.countScansSince(since, FoodSource.BARCODE.name) < FoodGate.FREE_BARCODE_SCANS_PER_DAY
        } else {
            val ai = dao.countScansSince(since, FoodSource.PHOTO.name) + dao.countScansSince(since, FoodSource.LABEL.name)
            ai < FoodGate.FREE_AI_SCANS_PER_DAY
        }
    }

    /** Opens the paywall for a scan that hit the free limit. */
    fun showScanPaywall(source: FoodSource) = pro.requestPaywall(
        if (source == FoodSource.BARCODE) com.rave.app.data.billing.ProReason.SCANS else com.rave.app.data.billing.ProReason.AI_SCANS,
    )

    fun slotNow(): MealSlot {
        val h = LocalTime.now().hour
        return when (h) {
            in 4..10 -> MealSlot.BREAKFAST
            in 11..15 -> MealSlot.LUNCH
            in 16..18 -> MealSlot.SNACK
            else -> MealSlot.DINNER
        }
    }
}
