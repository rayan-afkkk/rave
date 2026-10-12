package com.rave.app.di

import com.rave.app.data.cloud.SharedStreakRepository
import com.rave.app.data.repository.HabitCompletionListener
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    /** Habit completions feed shared streaks (no-op when signed out / no Firebase). */
    @Binds
    abstract fun bindCompletionListener(impl: SharedStreakRepository): HabitCompletionListener
}
