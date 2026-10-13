package com.rave.app.data.repository

import com.rave.app.core.TimeProvider
import com.rave.app.data.local.dao.StepsDao
import com.rave.app.data.local.entity.DailyStepsEntity
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.fitness.FitnessMath
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
 * Daily step totals built from the phone's cumulative step counter. Each reading adds the
 * steps since the previous one to today's row, so counting keeps going while the app is
 * closed (see StepSyncWorker) and survives reboots.
 */
@Singleton
class StepRepository @Inject constructor(
    private val dao: StepsDao,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) {
    private val mutex = Mutex()

    suspend fun record(counter: Long) = mutex.withLock {
        val previous = settings.getString(KEY_LAST_COUNTER)?.toLongOrNull()
        val delta = FitnessMath.counterDelta(previous, counter)
        settings.putString(KEY_LAST_COUNTER, counter.toString())
        if (delta <= 0) return@withLock
        val today = time.today().toString()
        val current = dao.get(today)?.steps ?: 0
        dao.upsert(DailyStepsEntity(today, (current + delta).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), time.nowMs()))
    }

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
    }
}
