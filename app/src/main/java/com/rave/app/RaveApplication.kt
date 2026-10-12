package com.rave.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.rave.app.core.ApplicationScope
import com.rave.app.data.repository.HabitRepository
import com.rave.app.data.repository.StreakRepository
import com.rave.app.notifications.Notifier
import com.rave.app.tracking.autodetect.AutoDetectManager
import com.rave.app.work.DailyStreakWorker
import com.rave.app.work.ReminderScheduler
import com.rave.app.work.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class RaveApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notifier: Notifier
    @Inject lateinit var streaks: StreakRepository
    @Inject lateinit var habits: HabitRepository
    @Inject lateinit var reminders: ReminderScheduler
    @Inject lateinit var sync: SyncScheduler
    @Inject lateinit var autoDetect: AutoDetectManager
    @Inject @field:ApplicationScope lateinit var scope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        notifier.createChannels()
        DailyStreakWorker.schedule(this)
        sync.schedulePeriodic()
        scope.launch {
            // Catch up on days closed while the app wasn't running, and re-arm reminders.
            streaks.recompute()
            reminders.rescheduleAll(habits.activeHabits())
            autoDetect.sync()
        }
    }
}
