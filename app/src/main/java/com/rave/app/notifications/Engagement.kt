package com.rave.app.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.rave.app.R
import com.rave.app.core.TimeProvider
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.HabitRepository
import com.rave.app.data.repository.StepRepository
import com.rave.app.data.repository.WeeklyReportRepository
import com.rave.app.domain.model.NotificationCategory
import com.rave.app.domain.model.ScheduleType
import com.rave.app.domain.notify.StepNudges
import com.rave.app.domain.notify.SunTimes
import com.rave.app.domain.util.Format
import com.rave.app.tracking.TrackingManager
import com.rave.app.ui.permissions.hasPermission
import com.rave.app.work.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/** Habit reminders, the evening step nudge and the Sunday weekly report (all fired by [AlarmReceiver]). */
@Singleton
class Engagement @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val habits: HabitRepository,
    private val steps: StepRepository,
    private val reports: WeeklyReportRepository,
    private val reminders: ReminderScheduler,
    private val tracking: TrackingManager,
    private val notifier: Notifier,
    private val alarms: RaveAlarms,
    private val time: TimeProvider,
) {
    /** Re-arms everything: app start, boot, time zone change, exact-alarm permission granted. */
    suspend fun rescheduleAll() {
        reminders.rescheduleAll(habits.activeHabits())
        scheduleStepNudge()
        scheduleWeeklyReport()
    }

    // ---- Habit reminders ----

    suspend fun habitReminder(habitId: Long, hhmm: String) {
        // Re-arm first so a crash below can't stop tomorrow's reminder.
        reminders.arm(habitId, hhmm, time.now().plusMinutes(1))
        val habit = habits.getHabit(habitId) ?: return
        if (habit.archived || !habit.remindersEnabled) return
        val today = time.today()
        val log = habits.logsFor(today)[habit.id]
        val due = habit.schedule.isScheduledOn(today) || habit.schedule.type == ScheduleType.TIMES_PER_WEEK
        if (!due || log?.completed == true || log?.skipped == true) return
        val notifId = Notifier.Ids.REMINDER_BASE + (habit.id.toInt() and 0xFFF)
        val builder = notifier.builder(Notifier.Channels.REMINDERS)
            .setContentTitle(context.getString(R.string.notif_reminder_title, habit.name))
            .setContentText(context.getString(R.string.notif_reminder_body))
            .setContentIntent(notifier.openIntent("rave://home"))
            .addAction(NotificationCompat.Action(0, context.getString(R.string.action_mark_done),
                HabitActionReceiver.completeIntent(context, habit.id, notifId)))
        notifier.post(NotificationCategory.REMINDERS, notifId, builder)
    }

    // ---- Evening step nudge ----

    suspend fun scheduleStepNudge(after: java.time.LocalDateTime = time.now()) {
        var date = after.toLocalDate()
        var at = date.atTime(nudgeTime(date))
        if (!at.isAfter(after)) {
            date = date.plusDays(1)
            at = date.atTime(nudgeTime(date))
        }
        alarms.set(AlarmReceiver.ACTION_STEP_NUDGE, "nudge", at, exact = false)
    }

    suspend fun stepNudge() {
        scheduleStepNudge(time.now().plusMinutes(1))
        if (tracking.state.value.isTracking) return
        val s = settings.current()
        val nudge = StepNudges.evaluate(steps.todaySteps(), s.dailyStepGoal) ?: return
        val maghrib = sunsetFor(time.today()) != null
        val n = NumberFormat.getIntegerInstance()
        val title = context.getString(R.string.nudge_title, n.format(nudge.stepsShort))
        val body = context.getString(
            when {
                nudge.closesGap && maghrib -> R.string.nudge_body_maghrib
                nudge.closesGap -> R.string.nudge_body_evening
                maghrib -> R.string.nudge_body_most_maghrib
                else -> R.string.nudge_body_most_evening
            },
            nudge.walkMinutes,
        )
        val builder = notifier.builder(Notifier.Channels.REMINDERS)
            .setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(notifier.openIntent("rave://home"))
            .addAction(NotificationCompat.Action(0, context.getString(R.string.nudge_start_walk), notifier.openIntent("rave://start/walk")))
        notifier.post(NotificationCategory.STEP_NUDGES, Notifier.Ids.STEP_NUDGE, builder)
    }

    private suspend fun nudgeTime(date: LocalDate): LocalTime = StepNudges.nudgeTime(sunsetFor(date))

    private suspend fun sunsetFor(date: LocalDate): LocalTime? {
        val (lat, lng) = lastLocation() ?: return null
        return SunTimes.sunset(date, lat, lng, time.zone)
    }

    /** Last known position (no GPS fix is requested); remembered so it works without permission later. */
    @SuppressLint("MissingPermission")
    private suspend fun lastLocation(): Pair<Double, Double>? {
        if (context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) || context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            val loc = withTimeoutOrNull(3_000) {
                runCatching { LocationServices.getFusedLocationProviderClient(context).lastLocation.await() }.getOrNull()
            }
            if (loc != null) {
                settings.putString(KEY_LAST_LOC, "${loc.latitude},${loc.longitude}")
                return loc.latitude to loc.longitude
            }
        }
        val saved = settings.getString(KEY_LAST_LOC)?.split(',')?.mapNotNull { it.toDoubleOrNull() }
        return if (saved?.size == 2) saved[0] to saved[1] else null
    }

    // ---- Sunday weekly report ----

    fun scheduleWeeklyReport(after: java.time.LocalDateTime = time.now()) {
        var at = after.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(WEEKLY_TIME)
        if (!at.isAfter(after)) at = at.plusWeeks(1)
        alarms.set(AlarmReceiver.ACTION_WEEKLY, "weekly", at, exact = false)
    }

    suspend fun weeklyReport() {
        scheduleWeeklyReport(time.now().plusMinutes(1))
        val r = reports.build()
        if (r.isEmpty) return
        val unit = settings.current().unit
        val n = NumberFormat.getIntegerInstance()
        val change = if (r.leadsWithDistance) r.distanceChange else r.stepsChange
        val lead = if (r.leadsWithDistance) {
            context.getString(R.string.weekly_title_distance, Format.distance(r.distanceM, unit, 1), com.rave.app.ui.unitLabelText(context, unit))
        } else {
            context.getString(R.string.weekly_title_steps, n.format(r.steps))
        }
        val title = when {
            change == null -> lead
            change >= 0 -> context.getString(R.string.weekly_up, lead, change)
            else -> context.getString(R.string.weekly_down, lead, -change)
        }
        val body = context.getString(R.string.weekly_body, n.format(r.steps), r.activities, r.habitRate ?: 0)
        val builder = notifier.builder(Notifier.Channels.ACTIVITY)
            .setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(notifier.openIntent("rave://weekly"))
        notifier.post(NotificationCategory.WEEKLY_REPORT, Notifier.Ids.WEEKLY_REPORT, builder)
    }

    private companion object {
        const val KEY_LAST_LOC = "last_location"
        val WEEKLY_TIME: LocalTime = LocalTime.of(19, 0)
    }
}
