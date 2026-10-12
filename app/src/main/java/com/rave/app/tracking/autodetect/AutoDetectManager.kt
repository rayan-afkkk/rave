package com.rave.app.tracking.autodetect

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.rave.app.R
import com.rave.app.core.TimeProvider
import com.rave.app.data.local.dao.ActivityDao
import com.rave.app.data.local.dao.LocationPointDao
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.data.local.entity.LocationPointEntity
import com.rave.app.data.local.toTrackPoint
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.ActivityProcessor
import com.rave.app.domain.model.ActivityStatus
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.NotificationCategory
import com.rave.app.domain.model.TrackPoint
import com.rave.app.domain.tracking.FilterResult
import com.rave.app.domain.tracking.TrackFilter
import com.rave.app.domain.tracking.TrackStats
import com.rave.app.domain.util.Format
import com.rave.app.notifications.Notifier
import com.rave.app.tracking.TrackingManager
import com.rave.app.ui.unitRes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Background walk detection. Activity Recognition tells us when a walk starts/ends; only
 * during that window do we request low-power location updates. A walk of 10+ minutes
 * becomes a "Looks like you walked 1.2 km — log it?" notification.
 */
@Singleton
class AutoDetectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityDao: ActivityDao,
    private val pointDao: LocationPointDao,
    private val processor: ActivityProcessor,
    private val tracking: TrackingManager,
    private val settings: SettingsRepository,
    private val notifier: Notifier,
    private val time: TimeProvider,
) {
    private val filter = TrackFilter(maxAccuracyM = 40f)

    fun hasRecognitionPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    private fun hasBackgroundLocation(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine && background
    }

    /** Registers (or removes) walking transition updates according to the setting. */
    @SuppressLint("MissingPermission")
    suspend fun sync() {
        val enabled = settings.current().autoDetectWalks && hasRecognitionPermission()
        val client = ActivityRecognition.getClient(context)
        runCatching {
            if (enabled) {
                val transitions = listOf(ActivityTransition.ACTIVITY_TRANSITION_ENTER, ActivityTransition.ACTIVITY_TRANSITION_EXIT).map {
                    ActivityTransition.Builder().setActivityType(DetectedActivity.WALKING).setActivityTransition(it).build()
                }
                client.requestActivityTransitionUpdates(ActivityTransitionRequest(transitions), transitionIntent()).await()
            } else {
                client.removeActivityTransitionUpdates(transitionIntent()).await()
            }
        }
    }

    /** Walking started: open a draft activity and start low-power location updates. */
    @SuppressLint("MissingPermission")
    suspend fun onWalkStarted() {
        if (!settings.current().autoDetectWalks) return
        if (tracking.state.value.isTracking || activityDao.getDetecting() != null) return
        activityDao.insert(
            ActivityEntity(type = ActivityType.WALK, status = ActivityStatus.DETECTING, startTime = time.nowMs(), autoDetected = true),
        )
        if (!hasBackgroundLocation()) return
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30_000)
            .setMinUpdateIntervalMillis(15_000)
            .build()
        runCatching { LocationServices.getFusedLocationProviderClient(context).requestLocationUpdates(request, locationIntent()).await() }
    }

    suspend fun onLocations(locations: List<android.location.Location>) {
        val draft = activityDao.getDetecting() ?: return
        var last: TrackPoint? = pointDao.forActivity(draft.id).lastOrNull()?.toTrackPoint()
        for (loc in locations) {
            val p = TrackPoint(loc.latitude, loc.longitude, null, loc.accuracy, if (loc.hasSpeed()) loc.speed else null, loc.time)
            if (filter.evaluate(last, p) is FilterResult.Accept) {
                pointDao.insert(LocationPointEntity(activityId = draft.id, lat = p.lat, lng = p.lng, accuracy = p.accuracy, speed = p.speed, timestamp = p.timeMs))
                last = p
            }
        }
    }

    /** Walking ended: stop updates and ask the user whether to log it. */
    suspend fun onWalkEnded() {
        runCatching { LocationServices.getFusedLocationProviderClient(context).removeLocationUpdates(locationIntent()).await() }
        val draft = activityDao.getDetecting() ?: return
        val points = pointDao.forActivity(draft.id).map { it.toTrackPoint() }
        val durationMs = time.nowMs() - draft.startTime
        val distance = TrackStats.distance(points)
        if (durationMs < MIN_WALK_MS || distance < MIN_DISTANCE_M) {
            activityDao.delete(draft.id)
            return
        }
        val unit = settings.current().unit
        val text = context.getString(R.string.notif_auto_detect_body, Format.distance(distance, unit, 1), context.getString(unitRes(unit)))
        val builder = notifier.builder(Notifier.Channels.ACTIVITY)
            .setContentTitle(context.getString(R.string.notif_auto_detect_title))
            .setContentText(text)
            .addAction(NotificationCompat.Action(0, context.getString(R.string.action_log_it), actionIntent(ACTION_LOG, draft.id)))
            .addAction(NotificationCompat.Action(0, context.getString(R.string.action_dismiss), actionIntent(ACTION_DISMISS, draft.id)))
            .setDeleteIntent(actionIntent(ACTION_DISMISS, draft.id))
        notifier.post(NotificationCategory.AUTO_DETECT, Notifier.Ids.AUTO_DETECT, builder)
    }

    suspend fun log(activityId: Long) {
        notifier.cancel(Notifier.Ids.AUTO_DETECT)
        processor.finalize(activityId, autoDetected = true)
    }

    suspend fun dismiss(activityId: Long) {
        notifier.cancel(Notifier.Ids.AUTO_DETECT)
        val a = activityDao.getById(activityId) ?: return
        if (a.status == ActivityStatus.DETECTING) activityDao.delete(activityId)
    }

    private fun transitionIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, 10, Intent(context, AutoDetectReceiver::class.java).setAction(AutoDetectReceiver.ACTION_TRANSITION),
        // Activity Recognition fills in extras, so this PendingIntent must be mutable.
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    private fun locationIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, 11, Intent(context, AutoDetectReceiver::class.java).setAction(AutoDetectReceiver.ACTION_LOCATION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    private fun actionIntent(action: String, activityId: Long): PendingIntent = PendingIntent.getBroadcast(
        context, action.hashCode() + activityId.toInt(),
        Intent(context, AutoDetectReceiver::class.java).setAction(action).putExtra(AutoDetectReceiver.EXTRA_ACTIVITY_ID, activityId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val ACTION_LOG = "com.rave.app.autodetect.LOG"
        const val ACTION_DISMISS = "com.rave.app.autodetect.DISMISS"
        private const val MIN_WALK_MS = 10 * 60_000L
        private const val MIN_DISTANCE_M = 300.0
    }
}
