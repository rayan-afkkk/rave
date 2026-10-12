package com.rave.app.core

import android.content.Intent
import android.net.Uri
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import android.content.Context
import com.rave.app.BuildConfig
import com.rave.app.data.preferences.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Where an incoming link should take the user. */
sealed interface DeepLink {
    data class Invite(val code: String) : DeepLink
    data class CompetitionLink(val id: String) : DeepLink
    data class ActivityLink(val id: Long, val isNew: Boolean) : DeepLink
    data object Tracking : DeepLink
    data object Home : DeepLink
    data object Profile : DeepLink
    data object Friends : DeepLink
    data object FriendRequests : DeepLink
    data object SharedStreaks : DeepLink
    data class StartActivity(val type: com.rave.app.domain.model.ActivityType) : DeepLink
}

/**
 * Parses App Links (`https://<domain>/invite/{code}`, `/competition/{id}`) and internal
 * `rave://…` links from notifications. The pending link is consumed by the UI once ready
 * (after onboarding / sign-in when needed).
 */
@Singleton
class DeepLinkHandler @Inject constructor(
    private val settings: SettingsRepository,
) {
    private val _pending = MutableStateFlow<DeepLink?>(null)
    val pending: StateFlow<DeepLink?> = _pending.asStateFlow()

    suspend fun handle(intent: Intent?) {
        val uri = intent?.data ?: return
        val link = parse(uri) ?: return
        // Keep invite codes across sign-in / process death.
        if (link is DeepLink.Invite) settings.setPendingInvite(link.code)
        _pending.value = link
    }

    fun consume() {
        _pending.value = null
    }

    fun parse(uri: Uri): DeepLink? {
        val segments = uri.pathSegments
        return when (uri.scheme) {
            "https", "http" -> {
                if (uri.host != BuildConfig.INVITE_DOMAIN) return null
                when (segments.getOrNull(0)) {
                    "invite" -> segments.getOrNull(1)?.let { DeepLink.Invite(it) }
                    "competition" -> segments.getOrNull(1)?.let { DeepLink.CompetitionLink(it) }
                    else -> null
                }
            }
            "rave" -> when (uri.host) {
                "invite" -> segments.getOrNull(0)?.let { DeepLink.Invite(it) }
                "competition" -> segments.getOrNull(0)?.let { DeepLink.CompetitionLink(it) }
                "activity" -> segments.getOrNull(0)?.toLongOrNull()?.let {
                    DeepLink.ActivityLink(it, uri.getQueryParameter("new") == "true")
                }
                "tracking" -> DeepLink.Tracking
                "start" -> DeepLink.StartActivity(
                    if (segments.firstOrNull() == "run") com.rave.app.domain.model.ActivityType.RUN else com.rave.app.domain.model.ActivityType.WALK,
                )
                "profile" -> DeepLink.Profile
                "friends" -> when (segments.getOrNull(0)) {
                    "requests" -> DeepLink.FriendRequests
                    "streaks" -> DeepLink.SharedStreaks
                    else -> DeepLink.Friends
                }
                else -> DeepLink.Home
            }
            else -> null
        }
    }

    /**
     * Deferred deep link for people who installed from an invite: the website forwards to the
     * Play Store with `referrer=invite_code%3DRAYAN-7K2Q`; read once on first launch.
     */
    suspend fun checkInstallReferrer(context: Context) {
        if (settings.isInstallReferrerChecked()) return
        val referrer = suspendCancellableCoroutine { cont ->
            val client = InstallReferrerClient.newBuilder(context).build()
            client.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(code: Int) {
                    val value = if (code == InstallReferrerClient.InstallReferrerResponse.OK) {
                        runCatching { client.installReferrer.installReferrer }.getOrNull()
                    } else null
                    runCatching { client.endConnection() }
                    if (cont.isActive) cont.resume(value)
                }

                override fun onInstallReferrerServiceDisconnected() {
                    if (cont.isActive) cont.resume(null)
                }
            })
        }
        settings.setInstallReferrerChecked()
        val code = referrer?.split('&')?.firstOrNull { it.startsWith("invite_code=") }?.substringAfter('=')
        if (!code.isNullOrBlank()) {
            settings.setPendingInvite(code)
            _pending.value = DeepLink.Invite(code)
        }
    }
}
