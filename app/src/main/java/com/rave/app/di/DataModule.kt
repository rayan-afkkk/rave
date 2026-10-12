package com.rave.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.rave.app.core.ApplicationScope
import com.rave.app.core.createApplicationScope
import com.rave.app.data.local.AppDatabase
import com.rave.app.data.local.dao.ActivityDao
import com.rave.app.data.local.dao.HabitDao
import com.rave.app.data.local.dao.HabitLogDao
import com.rave.app.data.local.dao.LocationPointDao
import com.rave.app.data.local.dao.PrivacyZoneDao
import com.rave.app.data.local.dao.StatsDao
import com.rave.app.data.local.dao.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides fun provideHabitDao(db: AppDatabase): HabitDao = db.habitDao()
    @Provides fun provideHabitLogDao(db: AppDatabase): HabitLogDao = db.habitLogDao()
    @Provides fun provideActivityDao(db: AppDatabase): ActivityDao = db.activityDao()
    @Provides fun provideLocationPointDao(db: AppDatabase): LocationPointDao = db.locationPointDao()
    @Provides fun provideStatsDao(db: AppDatabase): StatsDao = db.statsDao()
    @Provides fun providePrivacyZoneDao(db: AppDatabase): PrivacyZoneDao = db.privacyZoneDao()
    @Provides fun provideWorkoutDao(db: AppDatabase): WorkoutDao = db.workoutDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("user_prefs") }

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = createApplicationScope()
}
