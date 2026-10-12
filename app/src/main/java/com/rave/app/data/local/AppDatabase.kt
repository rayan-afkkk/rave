package com.rave.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rave.app.data.local.dao.ActivityDao
import com.rave.app.data.local.dao.HabitDao
import com.rave.app.data.local.dao.HabitLogDao
import com.rave.app.data.local.dao.LocationPointDao
import com.rave.app.data.local.dao.PrivacyZoneDao
import com.rave.app.data.local.dao.StatsDao
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.data.local.entity.BadgeEntity
import com.rave.app.data.local.entity.CompetitionWinEntity
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
    ],
    version = 1,
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

    companion object {
        const val NAME = "rave.db"
    }
}
