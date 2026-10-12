package com.rave.app.tracking

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.rave.app.R
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.util.Format
import com.rave.app.notifications.Notifier
import com.rave.app.ui.activityTypeRes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service (type `location`) that keeps GPS running with the screen off.
 * Shows live distance + time with Pause/Resume and Stop actions.
 */
@AndroidEntryPoint
class TrackingService : Service() {
    @Inject lateinit var manager: TrackingManager
    @Inject lateinit var notifier: Notifier
    @Inject lateinit var settings: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var started = false
    private var lastNotificationAt = 0L
    private var unit = DistanceUnit.KM

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach(manager::onLocation)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must enter the foreground quickly after startForegroundService().
        if (!enterForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_PAUSE -> scope.launch { manager.pause() }
            ACTION_RESUME -> scope.launch { manager.resume() }
            ACTION_STOP -> scope.launch { finishFromNotification() }
        }
        if (!started) {
            started = true
            scope.launch {
                // A sticky restart after process death arrives with a null intent.
                if (!manager.restore()) {
                    stopSelf()
                    return@launch
                }
                observe()
            }
        }
        return START_STICKY
    }

    private fun enterForeground(): Boolean = try {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        ServiceCompat.startForeground(this, Notifier.Ids.TRACKING, buildNotification(manager.state.value), type)
        true
    } catch (e: SecurityException) {
        // Location permission was revoked; we can't run a location service.
        false
    } catch (e: IllegalStateException) {
        // Background start not allowed (ForegroundServiceStartNotAllowedException on 12+).
        false
    }

    private fun observe() {
        scope.launch {
            manager.locationMode.collectLatest { mode -> requestUpdates(mode) }
        }
        scope.launch {
            manager.state.collectLatest { state ->
                if (!state.isTracking) {
                    stopForegroundCompat()
                    stopSelf()
                    return@collectLatest
                }
                val now = System.currentTimeMillis()
                // Throttle notification updates to ~1/s.
                if (now - lastNotificationAt >= 1_000) {
                    lastNotificationAt = now
                    notifier.postNow(Notifier.Ids.TRACKING, notificationBuilder(state))
                }
            }
        }
        scope.launch {
            settings.settings.map { it.unit }.distinctUntilChanged().collectLatest { unit = it }
        }
        scope.launch {
            settings.settings.map { it.batterySaverGps }.distinctUntilChanged().collectLatest {
                requestUpdates(manager.locationMode.value)
            }
        }
    }

    @SuppressLint("MissingPermission") // The service only starts after permission is granted.
    private fun requestUpdates(mode: LocationMode) {
        fused.removeLocationUpdates(callback)
        val priority = if (mode.highAccuracy) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val request = LocationRequest.Builder(priority, mode.intervalMs)
            .setMinUpdateIntervalMillis(mode.intervalMs / 2)
            .setWaitForAccurateLocation(false)
            .build()
        try {
            fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private suspend fun finishFromNotification() {
        val result = manager.finish() ?: return
        notifier.postNow(
            Notifier.Ids.ACTIVITY_SAVED,
            notifier.builder(Notifier.Channels.ACTIVITY)
                .setContentTitle(getString(R.string.notif_activity_saved_title))
                .setContentText(getString(R.string.notif_activity_saved_body))
                .setContentIntent(notifier.openIntent("rave://activity/${result.activityId}?new=true")),
        )
    }

    private fun buildNotification(state: TrackingState): Notification = notificationBuilder(state).build()

    private fun notificationBuilder(state: TrackingState): NotificationCompat.Builder {
        val unitRes = if (unit == DistanceUnit.MILES) R.string.unit_mi else R.string.unit_km
        val distance = Format.distance(state.distanceM, unit)
        val title = getString(R.string.tracking_notification_title, getString(activityTypeRes(state.type)), distance, getString(unitRes))
        val text = when (state.status) {
            TrackingStatus.PAUSED -> getString(R.string.tracking_paused)
            TrackingStatus.AUTO_PAUSED -> getString(R.string.tracking_auto_paused)
            else -> getString(R.string.tracking_notification_time, Format.duration(state.elapsedMs / 1000))
        }
        val builder = NotificationCompat.Builder(this, Notifier.Channels.TRACKING)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(this, R.color.ic_launcher_background))
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(notifier.openIntent("rave://tracking"))
        if (state.status == TrackingStatus.ACTIVE) {
            builder.setUsesChronometer(true).setWhen(System.currentTimeMillis() - state.elapsedMs).setShowWhen(true)
            builder.addAction(0, getString(R.string.action_pause), serviceIntent(ACTION_PAUSE))
        } else {
            builder.setShowWhen(false)
            builder.addAction(0, getString(R.string.action_resume), serviceIntent(ACTION_RESUME))
        }
        builder.addAction(0, getString(R.string.action_stop), serviceIntent(ACTION_STOP))
        return builder
    }

    private fun serviceIntent(action: String): PendingIntent = PendingIntent.getService(
        this, action.hashCode(), Intent(this, TrackingService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun stopForegroundCompat() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        fused.removeLocationUpdates(callback)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_PAUSE = "com.rave.app.tracking.PAUSE"
        private const val ACTION_RESUME = "com.rave.app.tracking.RESUME"
        private const val ACTION_STOP = "com.rave.app.tracking.STOP"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TrackingService::class.java))
        }
    }
}
