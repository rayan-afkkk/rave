package com.rave.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rave.app.data.repository.StatsRepository
import com.rave.app.data.repository.StreakRepository
import com.rave.app.notifications.Notifier
import com.rave.app.widget.WidgetRefresher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Runs shortly after midnight: closes yesterday (streaks, freezes), awards badges, refreshes
 * the widget and tells the user if a freeze was used or a streak was lost. Re-arms itself.
 */
@HiltWorker
class DailyStreakWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val streaks: StreakRepository,
    private val stats: StatsRepository,
    private val notifier: Notifier,
    private val widget: WidgetRefresher,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val update = streaks.recompute()
        when {
            update.newFreezeUses.isNotEmpty() -> notifier.freezeUsed()
            update.previousOverall >= 2 && update.replay.overall.current == 0 -> notifier.streakLost(update.previousOverall)
        }
        stats.checkBadges()
        widget.refresh()
        schedule(applicationContext)
        return Result.success()
    }

    companion object {
        private const val NAME = "daily_streak"

        /** Schedules the next run at 00:10 local time. */
        fun schedule(context: Context, now: LocalDateTime = LocalDateTime.now()) {
            val next = now.toLocalDate().plusDays(1).atTime(0, 10)
            val delay = Duration.between(now, next).toMillis()
            val request = OneTimeWorkRequestBuilder<DailyStreakWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
