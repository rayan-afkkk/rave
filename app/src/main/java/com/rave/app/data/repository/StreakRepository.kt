package com.rave.app.data.repository

import com.rave.app.core.TimeProvider
import com.rave.app.data.local.dao.HabitDao
import com.rave.app.data.local.dao.HabitLogDao
import com.rave.app.data.local.dao.StatsDao
import com.rave.app.data.local.entity.FreezeInventoryEntity
import com.rave.app.data.local.entity.StreakEntity
import com.rave.app.data.local.toDomain
import com.rave.app.domain.habit.StreakEngine
import com.rave.app.domain.habit.StreakReplay
import com.rave.app.domain.habit.StreakState
import com.rave.app.data.preferences.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.drop
import javax.inject.Inject
import javax.inject.Singleton

/** Result of a streak recomputation, used by the daily worker to decide on notifications. */
data class StreakUpdate(
    val replay: StreakReplay,
    /** Freeze uses that happened since the last notified one. */
    val newFreezeUses: List<LocalDate>,
    /** Overall streak before this recompute (to detect a reset). */
    val previousOverall: Int,
)

/** A recently broken overall streak that can still be repaired. */
data class RepairOffer(val day: LocalDate, val lostStreak: Int)

/**
 * Caches streaks + freeze inventory derived by [StreakEngine.replay] (through yesterday).
 * Today's progress is layered on top in the UI via [StreakEngine.displayStreak].
 */
@Singleton
class StreakRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val logDao: HabitLogDao,
    private val statsDao: StatsDao,
    private val time: TimeProvider,
    private val settings: SettingsRepository,
    private val pro: com.rave.app.data.billing.ProRepository,
    @com.rave.app.core.ApplicationScope scope: kotlinx.coroutines.CoroutineScope,
) {
    private val mutex = Mutex()

    init {
        // Buying or losing Pro changes the monthly freezes, so replay the streaks.
        scope.launch { pro.isPro.drop(1).collect { recompute() } }
    }

    private val _repairOffer = MutableStateFlow<RepairOffer?>(null)
    /** Set when the overall streak broke yesterday or the day before and a repair is still available this month. */
    val repairOffer: StateFlow<RepairOffer?> = _repairOffer.asStateFlow()

    /** habitId → streak through yesterday. [StreakEntity.OVERALL] holds the overall streak. */
    fun observeStreaks(): Flow<Map<Long, StreakState>> = statsDao.observeStreaks().map { list ->
        list.associate { it.habitId to StreakState(it.current, it.best, it.lastCompletedDate?.let(LocalDate::ofEpochDay)) }
    }

    fun observeFreezes(): Flow<Int> = statsDao.observeFreezes().map { it?.count ?: 0 }

    suspend fun recompute(): StreakUpdate = mutex.withLock {
        val habits = habitDao.getAll().map { it.toDomain() }
        val logs = logDao.getAll().map { it.toDomain() }
        val inventory = statsDao.getFreezes() ?: FreezeInventoryEntity()
        val previousOverall = statsDao.getStreaks().firstOrNull { it.habitId == StreakEntity.OVERALL }?.current ?: 0

        val yesterday = time.today().minusDays(1)
        val repairs = repairedDays()
        val replay = StreakEngine.replay(habits, logs, yesterday, inventory.bonusGranted, monthlyFreezes = if (pro.isPro.value) PRO_MONTHLY_FREEZES else FREE_MONTHLY_FREEZES, repairedDays = repairs)
        val today = time.today()
        val brokeRecently = replay.lastBreak?.let { !it.isBefore(today.minusDays(2)) } == true
        val usedThisMonth = repairs.any { it.year == today.year && it.month == today.month }
        _repairOffer.value = if (brokeRecently && replay.lostStreak >= MIN_STREAK_TO_REPAIR && !usedThisMonth) {
            RepairOffer(replay.lastBreak!!, replay.lostStreak)
        } else null

        val rows = replay.perHabit.map { (id, s) ->
            StreakEntity(id, s.current, s.best, s.lastCompleted?.toEpochDay())
        } + StreakEntity(StreakEntity.OVERALL, replay.overall.current, replay.overall.best, replay.overall.lastCompleted?.toEpochDay())
        statsDao.replaceStreaks(rows)

        val lastNotified = inventory.lastNotifiedUseDay
        val newUses = replay.freezesUsedOn.filter { lastNotified == null || it.toEpochDay() > lastNotified }
        statsDao.upsertFreezes(
            inventory.copy(
                count = replay.freezesRemaining,
                lastNotifiedUseDay = replay.freezesUsedOn.lastOrNull()?.toEpochDay() ?: lastNotified,
            ),
        )
        StreakUpdate(replay, newUses, previousOverall)
    }

    /** Covers the missed day of [offer] (one repair per calendar month) and replays streaks. */
    suspend fun repair(offer: RepairOffer) {
        mutex.withLock {
            val days = repairedDays() + offer.day
            settings.putString(KEY_REPAIRS, days.sorted().joinToString(","))
        }
        recompute()
    }

    fun dismissRepairOffer() { _repairOffer.value = null }

    private suspend fun repairedDays(): Set<LocalDate> =
        settings.getString(KEY_REPAIRS).orEmpty().split(',').mapNotNull { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }.toSet()

    private companion object {
        const val KEY_REPAIRS = "streak_repairs"
        const val MIN_STREAK_TO_REPAIR = 2
        const val FREE_MONTHLY_FREEZES = 1
        const val PRO_MONTHLY_FREEZES = 3
    }

    /** Grants an extra freeze (e.g. invite reward). Capped by the engine at [StreakEngine.MAX_FREEZES]. */
    suspend fun grantBonusFreeze(count: Int = 1) {
        mutex.withLock {
            val inv = statsDao.getFreezes() ?: FreezeInventoryEntity()
            statsDao.upsertFreezes(inv.copy(bonusGranted = inv.bonusGranted + count))
        }
        recompute()
    }
}
