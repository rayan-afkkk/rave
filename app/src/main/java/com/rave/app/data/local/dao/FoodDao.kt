package com.rave.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rave.app.data.local.entity.FoodLogEntity
import com.rave.app.data.local.entity.FoodScanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Insert
    suspend fun insertLog(entry: FoodLogEntity): Long

    @Query("DELETE FROM food_log WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("SELECT * FROM food_log WHERE date = :date ORDER BY createdAt ASC")
    fun observeDay(date: String): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_log WHERE date BETWEEN :from AND :to")
    fun observeBetween(from: String, to: String): Flow<List<FoodLogEntity>>

    @Insert
    suspend fun insertScan(scan: FoodScanEntity): Long

    @Query("SELECT * FROM food_scans ORDER BY scannedAt DESC LIMIT :limit")
    fun observeRecentScans(limit: Int = 10): Flow<List<FoodScanEntity>>

    @Query("SELECT COUNT(*) FROM food_scans WHERE scannedAt >= :from AND source = :source")
    suspend fun countScansSince(from: Long, source: String): Int
}
