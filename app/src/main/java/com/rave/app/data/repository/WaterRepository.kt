package com.rave.app.data.repository

import com.rave.app.core.TimeProvider
import com.rave.app.data.preferences.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Glasses of water per day, kept in settings storage under `water_<date>`. */
@Singleton
class WaterRepository @Inject constructor(
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) {
    private val mutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeToday(): Flow<Int> = time.todayFlow.flatMapLatest { day ->
        settings.observeString(key(day.toString())).map { it?.toIntOrNull() ?: 0 }
    }

    /** Adds [delta] glasses to today (never below 0). */
    suspend fun add(delta: Int) = mutex.withLock {
        val k = key(time.today().toString())
        val now = (settings.getString(k)?.toIntOrNull() ?: 0) + delta
        settings.putString(k, now.coerceIn(0, MAX_GLASSES).toString())
    }

    private fun key(date: String) = "water_$date"

    private companion object {
        const val MAX_GLASSES = 30
    }
}
