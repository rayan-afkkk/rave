package com.rave.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.rave.app.data.local.entity.DailyStepsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StepsDao {
    @Upsert
    suspend fun upsert(day: DailyStepsEntity)

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    suspend fun get(date: String): DailyStepsEntity?

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    fun observe(date: String): Flow<DailyStepsEntity?>

    /** ISO dates sort correctly as text. */
    @Query("SELECT * FROM daily_steps WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeBetween(from: String, to: String): Flow<List<DailyStepsEntity>>
}
