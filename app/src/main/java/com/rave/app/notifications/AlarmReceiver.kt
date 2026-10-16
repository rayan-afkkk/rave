package com.rave.app.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Receives every Rave alarm (habit reminders, step nudge, weekly report) and re-arms after time changes. */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var engagement: Engagement

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_HABIT -> engagement.habitReminder(
                        intent.getLongExtra(EXTRA_HABIT_ID, -1),
                        intent.getStringExtra(EXTRA_TIME) ?: return@launch,
                    )
                    ACTION_STEP_NUDGE -> engagement.stepNudge()
                    ACTION_WEEKLY -> engagement.weeklyReport()
                    Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
                    AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> engagement.rescheduleAll()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_HABIT = "com.rave.app.alarm.HABIT"
        const val ACTION_STEP_NUDGE = "com.rave.app.alarm.STEP_NUDGE"
        const val ACTION_WEEKLY = "com.rave.app.alarm.WEEKLY"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_TIME = "time"
    }
}
