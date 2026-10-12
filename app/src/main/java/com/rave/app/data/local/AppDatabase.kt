package com.rave.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rave.app.data.local.dao.HabitDao
import com.rave.app.data.local.entity.HabitEntity

/**
 * Local, offline-first database. Habits and tracking work fully without network.
 * More entities (logs, activities, location points, streaks…) are added in later phases.
 */
@Database(
    entities = [HabitEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        const val NAME = "rave.db"
    }
}
