package com.rave.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AlarmManager wrapper. Reminders use exact alarms that fire even in Doze; WorkManager delays
 * (what reminders used before) are deferred for hours or dropped on many Android skins.
 * Without the "Alarms & reminders" permission it falls back to inexact allow-while-idle alarms.
 */
@Singleton
class RaveAlarms @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    fun canExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()

    /** Schedules [action] for [key] (a unique id like `habit/12/08:30`); replaces an earlier one with the same key. */
    fun set(action: String, key: String, at: LocalDateTime, exact: Boolean, extras: Intent.() -> Unit = {}) {
        val pi = PendingIntent.getBroadcast(context, 0, intent(action, key).apply(extras),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val atMs = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (exact && canExact()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi)
        }
    }

    fun cancel(action: String, key: String) {
        val pi = PendingIntent.getBroadcast(context, 0, intent(action, key), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            ?: return
        manager.cancel(pi)
        pi.cancel()
    }

    // The data URI makes each alarm's PendingIntent distinct.
    private fun intent(action: String, key: String) = Intent(context, AlarmReceiver::class.java)
        .setAction(action)
        .setData(Uri.parse("rave-alarm://$key"))
}
