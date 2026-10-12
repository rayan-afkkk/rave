package com.rave.app.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rave.app.core.TimeProvider
import com.rave.app.data.repository.HabitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Handles "Mark done" on reminder notifications (and from the widget). */
@AndroidEntryPoint
class HabitActionReceiver : BroadcastReceiver() {
    @Inject lateinit var habits: HabitRepository
    @Inject lateinit var notifier: Notifier
    @Inject lateinit var time: TimeProvider

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1)
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (habitId < 0) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                habits.getHabit(habitId)?.let { habits.complete(it, time.today()) }
                if (notifId >= 0) notifier.cancel(notifId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_COMPLETE = "com.rave.app.action.COMPLETE_HABIT"
        private const val EXTRA_HABIT_ID = "habit_id"
        private const val EXTRA_NOTIFICATION_ID = "notification_id"

        fun completeIntent(context: Context, habitId: Long, notificationId: Int): PendingIntent {
            val intent = Intent(context, HabitActionReceiver::class.java)
                .setAction(ACTION_COMPLETE)
                .putExtra(EXTRA_HABIT_ID, habitId)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            return PendingIntent.getBroadcast(
                context, habitId.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
