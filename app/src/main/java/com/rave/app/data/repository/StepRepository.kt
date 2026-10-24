package com.rave.app.data.repository

import com.rave.app.core.TimeProvider
import com.rave.app.data.local.dao.StepsDao
import com.rave.app.data.local.entity.DailyStepsEntity
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.fitness.FitnessMath
import com.rave.app.widget.WidgetRefresher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Daily step totals from the Recording API and the phone's cumulative step counter (see
 * [merge]). Readings happen when the app is open, from the widget and every ~15 minutes in
 * the background (StepSyncWorker), so counting keeps going while the app is closed.
 */
@Singleton
class StepRepository @Inject constructor(
    private val dao: StepsDao,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
    private val widget: WidgetRefresher,
) {
    private val mutex = Mutex()

    /** A live hardware-counter reading (app open, widget update). */
    suspend fun record(counter: Long) = merge(recorded = null, counter = counter)

    /**
     * Brings the daily totals up to date from both sources, never letting a day go down:
     * - [recorded]: per-day totals from the Recording API (counted by Play services in the
     *   background; can lag behind or miss steps on some phones),
     * - [counter]: the hardware step counter, whose difference since the last reading is
     *   added to today. This catches steps taken while the app was closed even when the
     *   Recording API hasn't delivered them.
     * Taking the larger of the two (rather than adding them) means steps are never counted
     * twice when both sources saw the same walk.
     */
    suspend fun merge(recorded: Map<LocalDate, Int>?, counter: Long?) = mutex.withLock {
        val today = time.today()
        var changed = false
        recorded?.forEach { (date, steps) ->
            if (date == today) return@forEach
            val key = date.toString()
            val current = dao.get(key)?.steps ?: 0
            if (steps > current) { dao.upsert(DailyStepsEntity(key, steps, time.nowMs())); changed = true }
        }
        var delta = 0L
        if (counter != null) {
            val previous = settings.getString(KEY_LAST_COUNTER)?.toLongOrNull()
            val sameDay = settings.getString(KEY_LAST_COUNTER_DAY) == today.toString()
            delta = FitnessMath.counterDelta(previous, counter)
            // The last reading was on an earlier day, so we can't tell which day these steps
            // belong to. If the Recording API is there it has the split; otherwise they go to today.
            if (!sameDay && previous != null && recorded != null) delta = 0
            settings.putString(KEY_LAST_COUNTER, counter.toString())
            settings.putString(KEY_LAST_COUNTER_DAY, today.toString())
        }
        val key = today.toString()
        val current = dao.get(key)?.steps ?: 0
        // The phone's own count is kept apart from the watch's, so a watch total that raised
        // today's number never gets phone steps added on top of it.
        val watch = dayValue(KEY_WATCH_TODAY, today)
        val phoneBefore = dayValue(KEY_PHONE_TODAY, today) ?: if (watch != null) 0 else current
        val phone = FitnessMath.mergeSteps(phoneBefore, recorded?.get(today), delta)
        settings.putString(KEY_PHONE_TODAY, "$today:$phone")
        val value = maxOf(current, phone, watch ?: 0)
        if (value != current) { dao.upsert(DailyStepsEntity(key, value, time.nowMs())); changed = true }
        if (changed) widget.refreshSteps()
    }

    /**
     * Per-day totals from a smartwatch (via Health Connect). Each day shows the larger of the
     * phone and watch counts, never their sum, and never goes down.
     */
    suspend fun mergeWatch(days: Map<LocalDate, Int>) = mutex.withLock {
        val today = time.today()
        var changed = false
        days.forEach { (date, steps) ->
            if (date.isAfter(today)) return@forEach
            if (date == today) settings.putString(KEY_WATCH_TODAY, "$today:$steps")
            val key = date.toString()
            val current = dao.get(key)?.steps ?: 0
            if (steps > current) { dao.upsert(DailyStepsEntity(key, steps, time.nowMs())); changed = true }
        }
        if (changed) widget.refreshSteps()
    }

    /** Reads a "yyyy-MM-dd:steps" value, or null if it's for another day. */
    private suspend fun dayValue(key: String, day: LocalDate): Int? {
        val raw = settings.getString(key) ?: return null
        val (date, steps) = raw.split(':').takeIf { it.size == 2 } ?: return null
        return if (date == day.toString()) steps.toIntOrNull() else null
    }

    suspend fun todaySteps(): Int = dao.get(time.today().toString())?.steps ?: 0

    suspend fun stepsBetween(from: LocalDate, to: LocalDate): Int = dao.sumBetween(from.toString(), to.toString())

    suspend fun bestDayBetween(from: LocalDate, to: LocalDate): Int = dao.getBetween(from.toString(), to.toString()).maxOfOrNull { it.steps } ?: 0

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeToday(): Flow<Int> = time.todayFlow.flatMapLatest { today ->
        dao.observe(today.toString()).map { it?.steps ?: 0 }
    }

    /** The last [days] days ending today, oldest first, with 0 for days without data. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeLastDays(days: Int): Flow<List<Pair<LocalDate, Int>>> = time.todayFlow.flatMapLatest { today ->
        val from = today.minusDays(days - 1L)
        dao.observeBetween(from.toString(), today.toString()).map { rows ->
            val byDate = rows.associate { it.date to it.steps }
            (0 until days).map { i -> from.plusDays(i.toLong()).let { d -> d to (byDate[d.toString()] ?: 0) } }
        }
    }

    private companion object {
        const val KEY_LAST_COUNTER = "steps_last_counter"
        const val KEY_LAST_COUNTER_DAY = "steps_last_counter_day"
        const val KEY_PHONE_TODAY = "steps_phone_today"
        const val KEY_WATCH_TODAY = "steps_watch_today"
    }
}
