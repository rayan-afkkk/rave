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
import com.rave.app.R
import com.rave.app.core.TimeProvider
import com.rave.app.data.local.dao.ActivityDao
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.ActivityProcessor
import com.rave.app.domain.model.ActivityStatus
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.NotificationCategory
import com.rave.app.domain.util.Format
import com.rave.app.notifications.Notifier
import com.rave.app.tracking.TrackingManager
import com.rave.app.ui.unitRes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Background walk detection without background location: Activity Recognition tells us when
 * a walk starts and ends, and the phone's step counter measures it (distance = steps × stride
 * from the user's height). A walk of 10+ minutes becomes a "Looks like you walked 1.2 km —
 * log it?" notification. Logged walks have no GPS route; live tracking is for routes.
 */
@Singleton
class AutoDetectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityDao: ActivityDao,
    private val processor: ActivityProcessor,
    private val tracking: TrackingManager,
    private val settings: SettingsRepository,
    private val notifier: Notifier,
    private val time: TimeProvider,
    private val stepSensor: com.rave.app.tracking.steps.StepSensor,
) {

    fun hasRecognitionPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

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

    /** Walking started: open a draft activity and remember the step counter. */
    suspend fun onWalkStarted() {
        if (!settings.current().autoDetectWalks) return
        if (tracking.state.value.isTracking || activityDao.getDetecting() != null) return
        activityDao.insert(
            ActivityEntity(type = ActivityType.WALK, status = ActivityStatus.DETECTING, startTime = time.nowMs(), autoDetected = true),
        )
        settings.putString(KEY_START_STEPS, stepSensor.readOnce()?.toString())
    }

    /** Walking ended: measure it from the step counter and ask the user whether to log it. */
    suspend fun onWalkEnded() {
        val draft = activityDao.getDetecting() ?: return
        val start = settings.getString(KEY_START_STEPS)?.toLongOrNull()
        val now = stepSensor.readOnce()
        settings.putString(KEY_START_STEPS, null)
        val steps = if (start != null && now != null && now >= start) (now - start).toInt() else 0
        val prefs = settings.current()
        val distance = com.rave.app.domain.fitness.FitnessMath.distanceM(steps, prefs.heightCm)
        val durationMs = time.nowMs() - draft.startTime
        if (durationMs < MIN_WALK_MS || distance < MIN_DISTANCE_M) {
            activityDao.delete(draft.id)
            return
        }
        activityDao.update(draft.copy(distanceM = distance, durationS = durationMs / 1000))
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
        private const val KEY_START_STEPS = "autodetect_start_steps"
    }
}
