package com.rave.app.tracking.steps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The phone's hardware step counter (TYPE_STEP_COUNTER): counts in a low-power chip all
 * the time and reports the total since boot, so the app only needs to read it now and then.
 */
@Singleton
class StepSensor @Inject constructor(@ApplicationContext private val context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    val isAvailable: Boolean get() = sensor != null

    /** Android 10+ needs the "Physical activity" permission to read steps. */
    fun hasPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    val canCount: Boolean get() = isAvailable && hasPermission()

    /** Live cumulative counter values while collected. */
    fun counter(): Flow<Long> = callbackFlow {
        val s = sensor ?: run { close(); return@callbackFlow }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) { trySend(event.values[0].toLong()) }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, s, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    /** One reading (the sensor reports its current total right after registering). */
    suspend fun readOnce(timeoutMs: Long = 5_000): Long? =
        if (!canCount) null else withTimeoutOrNull(timeoutMs) { counter().first() }
}
