package com.rave.app.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.rave.app.domain.model.Habit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules one WorkManager job per habit reminder time; each job re-arms itself for the next day. */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    fun schedule(habit: Habit) {
        cancel(habit.id)
        if (!habit.remindersEnabled || habit.archived) return
        habit.reminderTimes.forEach { enqueue(habit.id, it) }
    }

    fun rescheduleAll(habits: List<Habit>) = habits.forEach(::schedule)

    fun enqueue(habitId: Long, time: LocalTime, from: LocalDateTime = LocalDateTime.now()) {
        var next = from.toLocalDate().atTime(time)
        if (!next.isAfter(from)) next = next.plusDays(1)
        val delayMs = Duration.between(from, next).toMillis()
        val hhmm = time.format(FORMAT)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_HABIT_ID to habitId, ReminderWorker.KEY_TIME to hhmm))
            .addTag(tag(habitId))
            .build()
        workManager.enqueueUniqueWork("reminder_${habitId}_$hhmm", ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(habitId: Long) {
        workManager.cancelAllWorkByTag(tag(habitId))
    }

    private fun tag(habitId: Long) = "reminder_$habitId"

    companion object {
        val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
