package com.rave.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.data.local.entity.LocationPointEntity
import com.rave.app.domain.model.ActivityType
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Query("SELECT * FROM activities WHERE status = 'COMPLETED' ORDER BY startTime DESC")
    fun observeCompleted(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE status = 'COMPLETED' AND startTime BETWEEN :from AND :to ORDER BY startTime ASC")
    fun observeCompletedBetween(from: Long, to: Long): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE status = 'COMPLETED' AND startTime BETWEEN :from AND :to ORDER BY startTime ASC")
    suspend fun getCompletedBetween(from: Long, to: Long): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE status = 'COMPLETED'")
    suspend fun getAllCompleted(): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE id = :id")
    fun observeById(id: Long): Flow<ActivityEntity?>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getById(id: Long): ActivityEntity?

    @Query("SELECT * FROM activities WHERE status = 'COMPLETED' ORDER BY startTime DESC LIMIT 1")
    suspend fun latestCompleted(): ActivityEntity?

    @Query("SELECT * FROM activities WHERE status IN ('ACTIVE', 'PAUSED') ORDER BY startTime DESC LIMIT 1")
    suspend fun getInProgress(): ActivityEntity?

    @Query("SELECT * FROM activities WHERE status = 'DETECTING' ORDER BY startTime DESC LIMIT 1")
    suspend fun getDetecting(): ActivityEntity?

    @Query("SELECT * FROM activities WHERE status = 'COMPLETED' AND synced = 0")
    suspend fun getUnsynced(): List<ActivityEntity>

    @Insert
    suspend fun insert(activity: ActivityEntity): Long

    @Update
    suspend fun update(activity: ActivityEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE activities SET favorite = :favorite, synced = 0 WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE activities SET synced = 1, remoteId = :remoteId WHERE id = :id")
    suspend fun markSynced(id: Long, remoteId: String)

    @Query("UPDATE activities SET synced = 0")
    suspend fun markAllUnsynced()

    @Query("SELECT MIN(fastest1kS) FROM activities WHERE status = 'COMPLETED' AND type = :type AND id != :excludeId")
    suspend fun bestFastest1k(type: ActivityType, excludeId: Long): Double?

    @Query("SELECT MIN(fastest5kS) FROM activities WHERE status = 'COMPLETED' AND type = :type AND id != :excludeId")
    suspend fun bestFastest5k(type: ActivityType, excludeId: Long): Double?

    @Query("SELECT MAX(distanceM) FROM activities WHERE status = 'COMPLETED' AND type = :type AND id != :excludeId")
    suspend fun longest(type: ActivityType, excludeId: Long): Double?

    @Query("SELECT COUNT(*) FROM activities WHERE status = 'COMPLETED' AND type = :type AND id != :excludeId")
    suspend fun countOfType(type: ActivityType, excludeId: Long): Int
}

@Dao
interface LocationPointDao {

    @Insert
    suspend fun insert(point: LocationPointEntity): Long

    @Query("SELECT * FROM location_points WHERE activityId = :activityId ORDER BY timestamp ASC")
    suspend fun forActivity(activityId: Long): List<LocationPointEntity>

    @Query("SELECT * FROM location_points WHERE activityId = :activityId ORDER BY timestamp ASC")
    fun observeForActivity(activityId: Long): Flow<List<LocationPointEntity>>

    @Query("DELETE FROM location_points WHERE activityId = :activityId")
    suspend fun deleteForActivity(activityId: Long)
}
