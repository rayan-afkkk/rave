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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
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
) {
    private val mutex = Mutex()

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
        val replay = StreakEngine.replay(habits, logs, yesterday, inventory.bonusGranted)

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

    /** Grants an extra freeze (e.g. invite reward). Capped by the engine at [StreakEngine.MAX_FREEZES]. */
    suspend fun grantBonusFreeze(count: Int = 1) {
        mutex.withLock {
            val inv = statsDao.getFreezes() ?: FreezeInventoryEntity()
            statsDao.upsertFreezes(inv.copy(bonusGranted = inv.bonusGranted + count))
        }
        recompute()
    }
}
