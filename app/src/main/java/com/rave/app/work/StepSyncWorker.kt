package com.rave.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rave.app.data.repository.StepRepository
import com.rave.app.tracking.steps.StepSensor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Reads the step counter every 15 minutes so daily steps add up while the app is closed. */
@HiltWorker
class StepSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sensor: StepSensor,
    private val steps: StepRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        sensor.readOnce()?.let { steps.record(it) }
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
