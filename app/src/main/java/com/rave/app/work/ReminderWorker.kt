package com.rave.app.work

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rave.app.R
import com.rave.app.core.TimeProvider
import com.rave.app.data.repository.HabitRepository
import com.rave.app.domain.model.NotificationCategory
import com.rave.app.domain.model.ScheduleType
import com.rave.app.notifications.HabitActionReceiver
import com.rave.app.notifications.Notifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalTime

/** Fires a habit reminder if the habit is due today and not yet done, then re-arms for tomorrow. */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val habits: HabitRepository,
    private val scheduler: ReminderScheduler,
    private val notifier: Notifier,
    private val time: TimeProvider,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getLong(KEY_HABIT_ID, -1)
        val hhmm = inputData.getString(KEY_TIME) ?: return Result.failure()
        val habit = habits.getHabit(habitId) ?: return Result.success()
        if (habit.archived || !habit.remindersEnabled) return Result.success()

        val today = time.today()
        val log = habits.logsFor(today)[habit.id]
        val due = habit.schedule.isScheduledOn(today) || habit.schedule.type == ScheduleType.TIMES_PER_WEEK
        if (due && log?.completed != true && log?.skipped != true) {
            val notifId = Notifier.Ids.REMINDER_BASE + (habit.id.toInt() and 0xFFF)
            val builder = notifier.builder(Notifier.Channels.REMINDERS)
                .setContentTitle(applicationContext.getString(R.string.notif_reminder_title, habit.name))
                .setContentText(applicationContext.getString(R.string.notif_reminder_body))
                .setContentIntent(notifier.openIntent("rave://home"))
                .addAction(
                    NotificationCompat.Action(
                        0, applicationContext.getString(R.string.action_mark_done),
                        HabitActionReceiver.completeIntent(applicationContext, habit.id, notifId),
                    ),
                )
            notifier.post(NotificationCategory.REMINDERS, notifId, builder)
        }

        // Re-arm for the same time tomorrow.
        runCatching { LocalTime.parse(hhmm, ReminderScheduler.FORMAT) }.getOrNull()?.let {
            scheduler.enqueue(habit.id, it, time.now().plusMinutes(1))
        }
        return Result.success()
    }

    companion object {
        const val KEY_HABIT_ID = "habit_id"
        const val KEY_TIME = "time"
    }
}
