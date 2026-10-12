package com.rave.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.rave.app.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): HabitEntity?

    @Upsert
    suspend fun upsert(habit: HabitEntity): Long

    @Query("UPDATE habits SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
}
