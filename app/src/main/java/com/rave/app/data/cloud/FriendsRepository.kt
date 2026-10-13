package com.rave.app.data.cloud

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.rave.app.core.TimeProvider
import com.rave.app.data.preferences.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.tasks.await
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class Friend(
    val uid: String,
    val name: String,
    val username: String,
    val currentStreak: Int,
    val weekKm: Double,
    val photoUrl: String?,
)

enum class RequestStatus { PENDING, ACCEPTED, DECLINED }

data class FriendRequest(
    val id: String,
    val from: String,
    val to: String,
    val fromName: String,
    val toName: String,
    val status: RequestStatus,
    val viaInvite: Boolean,
)

data class FriendRequests(val incoming: List<FriendRequest> = emptyList(), val outgoing: List<FriendRequest> = emptyList())

/**
 * Friend graph: `users/{uid}/friends/{friendUid}` on both sides + `friendRequests/{from_to}`.
 * Nudges are `nudges/{from_to_yyyyMMdd}` so the "max 1 per friend per day" rule is enforced
 * by the document id (security rules only allow create, never overwrite).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FriendsRepository @Inject constructor(
    private val firebase: FirebaseProvider,
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) {
    private fun db() = firebase.requireFirestore()

    fun observeFriendIds(): Flow<List<String>> = auth.currentUser.flatMapLatest { user ->
        if (user == null || firebase.firestore == null) flowOf(emptyList())
        else db().collection(ProfileRepository.USERS).document(user.uid).collection("friends").observe()
            .map { s -> s.documents.map { it.id } }
            .catch { emit(emptyList()) }
    }

    fun observeFriends(): Flow<List<Friend>> = observeFriendIds().mapLatest { ids ->
        val weekStart = time.weekStart().toEpochDay()
        runCatching { profiles.getProfiles(ids) }.getOrDefault(emptyList()).map {
            Friend(it.uid, it.name, it.username, it.currentStreak, it.weekKmFor(weekStart), it.photoUrl)
        }.sortedByDescending { it.weekKm }
    }

    fun observeRequests(): Flow<FriendRequests> = auth.currentUser.flatMapLatest { user ->
        if (user == null || firebase.firestore == null) flowOf(FriendRequests())
        else {
            val col = db().collection(REQUESTS)
            val incoming = col.whereEqualTo("to", user.uid).whereEqualTo("status", RequestStatus.PENDING.name).observe()
                .map { s -> s.documents.mapNotNull { it.toRequest() } }
            val outgoing = col.whereEqualTo("from", user.uid).whereEqualTo("status", RequestStatus.PENDING.name).observe()
                .map { s -> s.documents.mapNotNull { it.toRequest() } }
            combine(incoming, outgoing) { i, o -> FriendRequests(i, o) }.catch { emit(FriendRequests()) }
        }
    }

    suspend fun sendRequest(toUid: String, toName: String, viaInvite: Boolean = false): Result<Unit> = runCatching {
        val me = firebase.requireUid()
        require(toUid != me) { "self" }
        val myName = settings.current().displayName
        // If they already asked us, accept instead of creating a mirror request.
        // Older security rules deny reading a request that doesn't exist; treat that as "no request".
        val reverse = runCatching { db().collection(REQUESTS).document("${toUid}_$me").get().await() }.getOrNull()
        if (reverse != null && reverse.exists() && reverse.getString("status") == RequestStatus.PENDING.name) {
            accept(reverse.toRequest()!!).getOrThrow()
            return@runCatching
        }
        db().collection(REQUESTS).document("${me}_$toUid").set(
            mapOf(
                "from" to me, "to" to toUid, "fromName" to myName, "toName" to toName,
                "status" to RequestStatus.PENDING.name, "viaInvite" to viaInvite, "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    /** Accepting writes the request status and both friend entries in one batch. */
    suspend fun accept(request: FriendRequest): Result<Unit> = runCatching {
        val me = firebase.requireUid()
        val users = db().collection(ProfileRepository.USERS)
        db().runBatch { b ->
            b.update(db().collection(REQUESTS).document(request.id), "status", RequestStatus.ACCEPTED.name)
            b.set(users.document(me).collection("friends").document(request.from), mapOf("since" to FieldValue.serverTimestamp(), "name" to request.fromName))
            b.set(users.document(request.from).collection("friends").document(me), mapOf("since" to FieldValue.serverTimestamp(), "name" to request.toName))
        }.await()
    }

    suspend fun decline(request: FriendRequest): Result<Unit> = runCatching {
        db().collection(REQUESTS).document(request.id).update("status", RequestStatus.DECLINED.name).await()
    }

    suspend fun cancel(request: FriendRequest): Result<Unit> = runCatching {
        db().collection(REQUESTS).document(request.id).delete().await()
    }

    suspend fun removeFriend(uid: String): Result<Unit> = runCatching {
        val me = firebase.requireUid()
        val users = db().collection(ProfileRepository.USERS)
        db().runBatch { b ->
            b.delete(users.document(me).collection("friends").document(uid))
            b.delete(users.document(uid).collection("friends").document(me))
        }.await()
    }

    private fun nudgeId(from: String, to: String) = "${from}_${to}_${time.today().format(DateTimeFormatter.BASIC_ISO_DATE)}"

    suspend fun hasNudgedToday(friendUid: String): Boolean {
        val me = firebase.uid ?: return false
        return runCatching { db().collection(NUDGES).document(nudgeId(me, friendUid)).get().await().exists() }.getOrDefault(false)
    }

    /** Records a nudge; the friend's app shows it on its next sync. Returns false if already nudged today. */
    suspend fun nudge(friendUid: String, reason: String, targetId: String? = null): Result<Boolean> = runCatching {
        val me = firebase.requireUid()
        val ref = db().collection(NUDGES).document(nudgeId(me, friendUid))
        if (ref.get().await().exists()) return@runCatching false
        ref.set(
            mapOf(
                "from" to me, "to" to friendUid, "fromName" to settings.current().displayName,
                "reason" to reason, "targetId" to targetId, "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        true
    }

    /** Nudges sent to me since [sinceMs] (shown as local notifications by the sync worker). */
    suspend fun nudgesSince(sinceMs: Long): List<Pair<String, String>> {
        val me = firebase.uid ?: return emptyList()
        return runCatching {
            db().collection(NUDGES).whereEqualTo("to", me)
                .whereGreaterThan("createdAt", com.google.firebase.Timestamp(java.util.Date(sinceMs)))
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(10).get().await()
                .documents.map { it.str("fromName") to it.str("reason") }
        }.getOrDefault(emptyList())
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toRequest(): FriendRequest? {
        if (!exists()) return null
        return FriendRequest(
            id = id, from = str("from"), to = str("to"), fromName = str("fromName"), toName = str("toName"),
            status = runCatching { RequestStatus.valueOf(str("status")) }.getOrDefault(RequestStatus.PENDING),
            viaInvite = getBoolean("viaInvite") == true,
        )
    }

    companion object {
        const val REQUESTS = "friendRequests"
        const val NUDGES = "nudges"
    }
}
