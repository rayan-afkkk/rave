package com.rave.app.work

import android.content.Context
import androidx.work.WorkManager
import com.rave.app.domain.model.Habit
import com.rave.app.notifications.AlarmReceiver
import com.rave.app.notifications.RaveAlarms
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One exact alarm per habit reminder time; [AlarmReceiver] posts the notification and re-arms
 * the next day. The armed times are remembered so editing a habit can cancel the old ones.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarms: RaveAlarms,
) {
    private val prefs get() = context.getSharedPreferences("rave_alarms", Context.MODE_PRIVATE)

    fun schedule(habit: Habit) {
        cancel(habit.id)
        if (!habit.remindersEnabled || habit.archived || habit.reminderTimes.isEmpty()) return
        val times = habit.reminderTimes.map { it.format(FORMAT) }.toSet()
        times.forEach { arm(habit.id, it) }
        prefs.edit().putStringSet(key(habit.id), times).apply()
    }

    fun rescheduleAll(habits: List<Habit>) = habits.forEach(::schedule)

    /** Arms the next occurrence of [hhmm] strictly after [from]. */
    fun arm(habitId: Long, hhmm: String, from: LocalDateTime = LocalDateTime.now()) {
        val time = runCatching { LocalTime.parse(hhmm, FORMAT) }.getOrNull() ?: return
        var next = from.toLocalDate().atTime(time)
        if (!next.isAfter(from)) next = next.plusDays(1)
        alarms.set(AlarmReceiver.ACTION_HABIT, "habit/$habitId/$hhmm", next, exact = true) {
            putExtra(AlarmReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(AlarmReceiver.EXTRA_TIME, hhmm)
        }
    }

    fun cancel(habitId: Long) {
        prefs.getStringSet(key(habitId), emptySet()).orEmpty().forEach { alarms.cancel(AlarmReceiver.ACTION_HABIT, "habit/$habitId/$it") }
        prefs.edit().remove(key(habitId)).apply()
        // Reminders used to be WorkManager jobs; clear any left from older versions.
        WorkManager.getInstance(context).cancelAllWorkByTag("reminder_$habitId")
    }

    private fun key(habitId: Long) = "habit_$habitId"

    companion object {
        val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
