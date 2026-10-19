package com.rave.app.tracking.steps

import com.rave.app.data.repository.StepRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Brings daily steps up to date from both the Recording API (Play services counts in the
 * background) and the hardware step counter; see StepRepository.merge.
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
        val days = if (recorded.isAvailable && recorded.subscribe()) recorded.daily(DAYS) else null
        // Always read the hardware counter too: some phones' Recording API misses or delays
        // steps taken while the app is closed.
        val counter = sensor.readOnce()
        if (days != null || counter != null) steps.merge(days, counter)
    }

    private companion object {
        const val DAYS = 7
    }
}
