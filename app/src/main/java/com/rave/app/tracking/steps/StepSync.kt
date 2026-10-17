package com.rave.app.tracking.steps

import com.rave.app.data.repository.StepRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Brings daily steps up to date. With the Recording API (most phones with Google Play) the
 * totals come straight from Play services, which counts in the background; otherwise the
 * hardware step counter is read and the difference since the last reading is added.
 */
@Singleton
class StepSync @Inject constructor(
    private val recorded: RecordedSteps,
    private val sensor: StepSensor,
    private val steps: StepRepository,
) {
    /** True when there's any way to count steps on this phone. */
    val canCount: Boolean get() = sensor.hasPermission() && (recorded.isAvailable || sensor.isAvailable)

    val usesRecording: Boolean get() = sensor.hasPermission() && recorded.isAvailable

    suspend fun sync() {
        if (!sensor.hasPermission()) return
        if (recorded.isAvailable && recorded.subscribe()) {
            recorded.daily(DAYS)?.let { steps.applyRecorded(it); return }
        }
        sensor.readOnce()?.let { steps.record(it) }
    }

    private companion object {
        const val DAYS = 7
    }
}
