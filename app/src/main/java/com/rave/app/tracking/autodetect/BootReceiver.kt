package com.rave.app.tracking.autodetect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rave.app.work.DailyStreakWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Activity-transition registrations don't survive a reboot; re-register them (and re-arm work). */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var autoDetect: AutoDetectManager
    @Inject lateinit var engagement: com.rave.app.notifications.Engagement

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                autoDetect.sync()
                engagement.rescheduleAll()
                DailyStreakWorker.schedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
