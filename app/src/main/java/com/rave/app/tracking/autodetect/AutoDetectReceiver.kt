package com.rave.app.tracking.autodetect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Receives activity transitions, background location batches and notification actions. */
@AndroidEntryPoint
class AutoDetectReceiver : BroadcastReceiver() {
    @Inject lateinit var manager: AutoDetectManager

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_TRANSITION -> if (ActivityTransitionResult.hasResult(intent)) {
                        ActivityTransitionResult.extractResult(intent)?.transitionEvents
                            ?.filter { it.activityType == DetectedActivity.WALKING }
                            ?.forEach { event ->
                                if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) manager.onWalkStarted()
                                else manager.onWalkEnded()
                            }
                    }
                    ACTION_LOCATION -> LocationResult.extractResult(intent)?.let { manager.onLocations(it.locations) }
                    AutoDetectManager.ACTION_LOG -> manager.log(intent.getLongExtra(EXTRA_ACTIVITY_ID, -1))
                    AutoDetectManager.ACTION_DISMISS -> manager.dismiss(intent.getLongExtra(EXTRA_ACTIVITY_ID, -1))
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TRANSITION = "com.rave.app.autodetect.TRANSITION"
        const val ACTION_LOCATION = "com.rave.app.autodetect.LOCATION"
        const val EXTRA_ACTIVITY_ID = "activity_id"
    }
}
