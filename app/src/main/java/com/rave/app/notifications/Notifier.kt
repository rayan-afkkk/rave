package com.rave.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rave.app.MainActivity
import com.rave.app.R
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.habit.BadgeId
import com.rave.app.domain.model.NotificationCategory
import com.rave.app.ui.badgeTitleRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Notification channels + helpers. Every post respects the user's per-category toggles. */
@Singleton
class Notifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) {
    object Channels {
        const val TRACKING = "tracking"
        const val REMINDERS = "reminders"
        const val FRIENDS = "friends"
        const val COMPETITIONS = "competitions"
        const val ACTIVITY = "activity"
    }

    object Ids {
        const val TRACKING = 1001
        const val AUTO_DETECT = 1002
        const val STREAK = 1003
        const val ACTIVITY_SAVED = 1004
        const val BADGE_BASE = 2000
        const val STEP_NUDGE = 1005
        const val WEEKLY_REPORT = 1006
        const val REMINDER_BASE = 3000
        const val SOCIAL_BASE = 4000
        const val COMPETITION_BASE = 5000
    }

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channels = listOf(
            NotificationChannel(Channels.TRACKING, context.getString(R.string.channel_tracking), NotificationManager.IMPORTANCE_LOW),
            NotificationChannel(Channels.REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(Channels.FRIENDS, context.getString(R.string.channel_friends), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(Channels.COMPETITIONS, context.getString(R.string.channel_competitions), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(Channels.ACTIVITY, context.getString(R.string.channel_activity), NotificationManager.IMPORTANCE_DEFAULT),
        )
        channels.forEach { it.setShowBadge(it.id != Channels.TRACKING) }
        manager.createNotificationChannels(channels)
    }

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Opens the app at an internal deep link such as `rave://activity/12`. */
    fun openIntent(deepLink: String, requestCode: Int = deepLink.hashCode()): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun builder(channel: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setAutoCancel(true)

    /** Posts unless the category is switched off or permission is missing. */
    suspend fun post(category: NotificationCategory?, id: Int, builder: NotificationCompat.Builder) {
        if (category != null && category !in settings.current().notifications) return
        postNow(id, builder)
    }

    @Suppress("MissingPermission") // checked by hasPermission()
    fun postNow(id: Int, builder: NotificationCompat.Builder) {
        if (!hasPermission()) return
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    fun cancel(id: Int) = NotificationManagerCompat.from(context).cancel(id)

    // ---- Convenience posts ----

    suspend fun badgeEarned(badge: BadgeId) {
        val title = context.getString(R.string.notif_badge_title)
        val body = context.getString(badgeTitleRes(badge))
        post(
            null,
            Ids.BADGE_BASE + badge.ordinal,
            builder(Channels.ACTIVITY).setContentTitle(title).setContentText(body).setContentIntent(openIntent("rave://profile")),
        )
    }

    suspend fun freezeUsed() = post(
        null, Ids.STREAK,
        builder(Channels.ACTIVITY)
            .setContentTitle(context.getString(R.string.notif_freeze_used_title))
            .setContentText(context.getString(R.string.notif_freeze_used_body))
            .setContentIntent(openIntent("rave://home")),
    )

    suspend fun streakLost(previous: Int) = post(
        NotificationCategory.REMINDERS, Ids.STREAK,
        builder(Channels.REMINDERS)
            .setContentTitle(context.getString(R.string.notif_streak_lost_title))
            .setContentText(context.getString(R.string.notif_streak_lost_body, previous))
            .setContentIntent(openIntent("rave://home")),
    )

    suspend fun social(id: Int, title: String, body: String, deepLink: String = "rave://friends") = post(
        NotificationCategory.FRIENDS, Ids.SOCIAL_BASE + (id and 0xFFF),
        builder(Channels.FRIENDS).setContentTitle(title).setContentText(body).setContentIntent(openIntent(deepLink)),
    )

    suspend fun competition(id: Int, title: String, body: String, competitionId: String) = post(
        NotificationCategory.COMPETITIONS, Ids.COMPETITION_BASE + (id and 0xFFF),
        builder(Channels.COMPETITIONS).setContentTitle(title).setContentText(body)
            .setContentIntent(openIntent("rave://competition/$competitionId")),
    )
}
