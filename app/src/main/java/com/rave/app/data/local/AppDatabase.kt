package com.rave.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rave.app.data.local.dao.StepsDao
import com.rave.app.data.local.dao.FoodDao
import com.rave.app.data.local.entity.FoodLogEntity
import com.rave.app.data.local.entity.FoodScanEntity
import com.rave.app.data.local.entity.DailyStepsEntity
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rave.app.data.local.dao.ActivityDao
import com.rave.app.data.local.dao.HabitDao
import com.rave.app.data.local.dao.HabitLogDao
import com.rave.app.data.local.dao.LocationPointDao
import com.rave.app.data.local.dao.PrivacyZoneDao
import com.rave.app.data.local.dao.StatsDao
import com.rave.app.data.local.dao.WorkoutDao
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.data.local.entity.BadgeEntity
import com.rave.app.data.local.entity.CompetitionWinEntity
import com.rave.app.data.local.entity.ExerciseEntity
import com.rave.app.data.local.entity.WorkoutEntity
import com.rave.app.data.local.entity.WorkoutSetEntity
import com.rave.app.data.local.entity.FreezeInventoryEntity
import com.rave.app.data.local.entity.HabitEntity
import com.rave.app.data.local.entity.HabitLogEntity
import com.rave.app.data.local.entity.LocationPointEntity
import com.rave.app.data.local.entity.PrivacyZoneEntity
import com.rave.app.data.local.entity.StreakEntity
import com.rave.app.data.local.entity.UserStatsEntity

/** Local, offline-first database. Habits and tracking work fully without network. */
@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        ActivityEntity::class,
        LocationPointEntity::class,
        StreakEntity::class,
        FreezeInventoryEntity::class,
        UserStatsEntity::class,
        BadgeEntity::class,
        PrivacyZoneEntity::class,
        CompetitionWinEntity::class,
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutSetEntity::class,
        DailyStepsEntity::class,
        FoodLogEntity::class,
        FoodScanEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun activityDao(): ActivityDao
    abstract fun locationPointDao(): LocationPointDao
    abstract fun statsDao(): StatsDao
    abstract fun privacyZoneDao(): PrivacyZoneDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun stepsDao(): StepsDao
    abstract fun foodDao(): FoodDao

    companion object {
        const val NAME = "rave.db"

        /** v3: food log and scan history. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `food_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, " +
                        "`slot` TEXT NOT NULL, `name` TEXT NOT NULL, `kcal` INTEGER NOT NULL, `protein` REAL NOT NULL, `carbs` REAL NOT NULL, " +
                        "`fat` REAL NOT NULL, `sugar` REAL, `source` TEXT NOT NULL, `verdict` TEXT, `barcode` TEXT, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `food_scans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `barcode` TEXT, " +
                        "`name` TEXT NOT NULL, `brand` TEXT NOT NULL, `verdict` TEXT NOT NULL, `kcal` INTEGER, `imageUrl` TEXT, " +
                        "`source` TEXT NOT NULL, `scannedAt` INTEGER NOT NULL)",
                )
            }
        }

        /** v2: daily step counts. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_steps` (`date` TEXT NOT NULL, `steps` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`date`))",
                )
            }
        }
    }
}
