package com.rave.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.rave.app.data.local.entity.HabitEntity
import com.rave.app.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY sortOrder ASC, createdAt ASC")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits")
    suspend fun getAll(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE archived = 0")
    suspend fun getActive(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE sharedStreakId = :sharedId LIMIT 1")
    suspend fun getBySharedStreak(sharedId: String): HabitEntity?

    @Upsert
    suspend fun upsert(habit: HabitEntity): Long

    @Query("UPDATE habits SET archived = 1, archivedEpochDay = :epochDay WHERE id = :id")
    suspend fun archive(id: Long, epochDay: Long)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun count(): Int
}

@Dao
interface HabitLogDao {

    @Query("SELECT * FROM habit_logs WHERE date = :epochDay")
    fun observeForDay(epochDay: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE date BETWEEN :from AND :to")
    fun observeBetween(from: Long, to: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE date BETWEEN :from AND :to")
    suspend fun getBetween(from: Long, to: Long): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE date = :epochDay")
    suspend fun getForDay(epochDay: Long): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs")
    suspend fun getAll(): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :epochDay")
    suspend fun get(habitId: Long, epochDay: Long): HabitLogEntity?

    @Upsert
    suspend fun upsert(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :epochDay")
    suspend fun delete(habitId: Long, epochDay: Long)

    @Query("SELECT COUNT(*) FROM habit_logs WHERE completed = 1")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM habit_logs WHERE completed = 1 AND date BETWEEN :from AND :to")
    fun observeCompletedCountBetween(from: Long, to: Long): Flow<Int>
}
