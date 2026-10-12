package com.rave.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rave.app.data.local.entity.BadgeEntity
import com.rave.app.data.local.entity.CompetitionWinEntity
import com.rave.app.data.local.entity.FreezeInventoryEntity
import com.rave.app.data.local.entity.PrivacyZoneEntity
import com.rave.app.data.local.entity.StreakEntity
import com.rave.app.data.local.entity.UserStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class StatsDao {

    // Streaks
    @Query("SELECT * FROM streaks")
    abstract fun observeStreaks(): Flow<List<StreakEntity>>

    @Query("SELECT * FROM streaks")
    abstract suspend fun getStreaks(): List<StreakEntity>

    @Query("DELETE FROM streaks")
    abstract suspend fun clearStreaks()

    @Upsert
    abstract suspend fun upsertStreaks(streaks: List<StreakEntity>)

    @Transaction
    open suspend fun replaceStreaks(streaks: List<StreakEntity>) {
        clearStreaks()
        upsertStreaks(streaks)
    }

    // Freezes
    @Query("SELECT * FROM freeze_inventory WHERE id = 0")
    abstract fun observeFreezes(): Flow<FreezeInventoryEntity?>

    @Query("SELECT * FROM freeze_inventory WHERE id = 0")
    abstract suspend fun getFreezes(): FreezeInventoryEntity?

    @Upsert
    abstract suspend fun upsertFreezes(inventory: FreezeInventoryEntity)

    // User stats
    @Query("SELECT * FROM user_stats WHERE id = 0")
    abstract fun observeUserStats(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 0")
    abstract suspend fun getUserStats(): UserStatsEntity?

    @Upsert
    abstract suspend fun upsertUserStats(stats: UserStatsEntity)

    // Badges
    @Query("SELECT * FROM badges")
    abstract fun observeBadges(): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badges")
    abstract suspend fun getBadges(): List<BadgeEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertBadges(badges: List<BadgeEntity>)

    // Competition wins
    @Query("SELECT * FROM competition_wins")
    abstract fun observeWins(): Flow<List<CompetitionWinEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertWin(win: CompetitionWinEntity): Long
}

@Dao
interface PrivacyZoneDao {
    @Query("SELECT * FROM privacy_zones")
    fun observeAll(): Flow<List<PrivacyZoneEntity>>

    @Query("SELECT * FROM privacy_zones")
    suspend fun getAll(): List<PrivacyZoneEntity>

    @Upsert
    suspend fun upsert(zone: PrivacyZoneEntity)

    @Query("DELETE FROM privacy_zones WHERE id = :id")
    suspend fun delete(id: Long)
}
