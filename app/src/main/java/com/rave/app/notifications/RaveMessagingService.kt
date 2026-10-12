package com.rave.app.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rave.app.data.cloud.ProfileRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles FCM data messages (type, title, body, targetId) if you add your own push sender.
 * The app works without it: SyncWorker raises the same notifications locally every hour.
 */
@AndroidEntryPoint
class RaveMessagingService : FirebaseMessagingService() {
    @Inject lateinit var profiles: ProfileRepository
    @Inject lateinit var notifier: Notifier

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { profiles.registerToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: return
        val body = data["body"] ?: message.notification?.body.orEmpty()
        val targetId = data["targetId"].orEmpty()
        scope.launch {
            when (data["type"]) {
                "competition" -> notifier.competition(targetId.hashCode() + 10, title, body, targetId)
                "shared_streak" -> notifier.social(targetId.hashCode(), title, body, "rave://friends/streaks")
                "friend_request" -> notifier.social(targetId.hashCode(), title, body, "rave://friends/requests")
                else -> notifier.social(title.hashCode(), title, body)
            }
        }
    }
}
