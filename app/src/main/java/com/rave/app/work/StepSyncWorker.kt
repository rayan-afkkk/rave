package com.rave.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rave.app.tracking.steps.StepSync
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Syncs daily steps every 15 minutes so they stay right while the app is closed (see StepSync). */
@HiltWorker
class StepSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val stepSync: StepSync,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        stepSync.sync()
        return Result.success()
    }

    companion object {
        private const val NAME = "step_sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StepSyncWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
