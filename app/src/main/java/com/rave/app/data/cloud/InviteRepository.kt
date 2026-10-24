package com.rave.app.data.cloud

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.rave.app.R
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.social.InviteCodes
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

enum class InviteType { APP, COMPETITION, SHARED_STREAK }

data class InviteInfo(
    val code: String,
    val type: InviteType,
    val targetId: String,
    val createdBy: String,
    val createdByName: String,
)

sealed interface RedeemResult {
    data class FriendRequested(val name: String) : RedeemResult
    data class JoinedCompetition(val competitionId: String) : RedeemResult
    data class JoinedSharedStreak(val name: String) : RedeemResult
    data object OwnCode : RedeemResult
}

/** Invite codes (`invites/{code}`), links (App Links) and share text. */
@Singleton
class InviteRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebase: FirebaseProvider,
    private val friends: FriendsRepository,
    private val profiles: ProfileRepository,
    private val competitions: Lazy<CompetitionRepository>,
    private val sharedStreaks: Lazy<SharedStreakRepository>,
    private val settings: SettingsRepository,
) {
    /** Invites are shared as a short code (and QR); there are no web invite links. */
    fun shareMessage(code: String): String =
        context.getString(R.string.invite_share_message_code_only, context.getString(R.string.app_name), code)

    suspend fun resolve(rawCode: String): InviteInfo? {
        val code = InviteCodes.normalize(rawCode)
        val doc = runCatching { firebase.requireFirestore().collection(ProfileRepository.INVITES).document(code).get().await() }.getOrNull()
        if (doc == null || !doc.exists()) return null
        return InviteInfo(
            code = code,
            type = runCatching { InviteType.valueOf(doc.str("type")) }.getOrDefault(InviteType.APP),
            targetId = doc.str("targetId"),
            createdBy = doc.str("createdBy"),
            createdByName = doc.str("createdByName"),
        )
    }

    /** Applies an invite for the signed-in user. */
    suspend fun redeem(info: InviteInfo): Result<RedeemResult> = runCatching {
        val me = firebase.requireUid()
        if (info.createdBy == me) return@runCatching RedeemResult.OwnCode
        when (info.type) {
            InviteType.APP -> {
                friends.sendRequest(info.createdBy, info.createdByName, viaInvite = true).getOrThrow()
                // Remember who invited us (for the "both get a freeze" reward). Only set once.
                val mine = profiles.getProfile(me)
                if (mine?.invitedBy == null) profiles.setFields(mapOf("invitedBy" to info.createdBy))
                RedeemResult.FriendRequested(info.createdByName)
            }
            InviteType.COMPETITION -> {
                competitions.get().join(info.targetId).getOrThrow()
                RedeemResult.JoinedCompetition(info.targetId)
            }
            InviteType.SHARED_STREAK -> {
                sharedStreaks.get().accept(info.targetId).getOrThrow()
                RedeemResult.JoinedSharedStreak(info.createdByName)
            }
        }
    }

    /** Creates a short code that points at a competition or shared streak. */
    suspend fun createTargetInvite(type: InviteType, targetId: String): String {
        val db = firebase.requireFirestore()
        val me = firebase.requireUid()
        val name = settings.current().displayName
        repeat(6) {
            val code = InviteCodes.generate(name)
            val ref = db.collection(ProfileRepository.INVITES).document(code)
            val created = db.runTransaction { tx ->
                if (tx.get(ref).exists()) false
                else {
                    tx.set(ref, mapOf("type" to type.name, "targetId" to targetId, "createdBy" to me, "createdByName" to name,
                        "createdAt" to FieldValue.serverTimestamp()))
                    true
                }
            }.await()
            if (created) return code
        }
        error("Could not allocate an invite code")
    }
}
