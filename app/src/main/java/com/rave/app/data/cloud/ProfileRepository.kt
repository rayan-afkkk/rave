package com.rave.app.data.cloud

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.rave.app.core.TimeProvider
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.model.ProfileVisibility
import com.rave.app.domain.social.InviteCodes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** Public profile + stats summary stored at `users/{uid}`. */
data class UserProfile(
    val uid: String,
    val name: String,
    val username: String,
    val inviteCode: String,
    val visibility: ProfileVisibility,
    val photoUrl: String? = null,
    val totalKm: Double = 0.0,
    val weekKm: Double = 0.0,
    val weekStartEpochDay: Long = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val pointsWeek: Int = 0,
    val invitedBy: String? = null,
) {
    /** This week's km, or 0 if the stored summary is from an older week. */
    fun weekKmFor(weekStartEpochDay: Long): Double = if (this.weekStartEpochDay == weekStartEpochDay) weekKm else 0.0
}

/** Summary pushed by the sync worker. */
data class StatsSummary(
    val totalKm: Double,
    val weekKm: Double,
    val weekStartEpochDay: Long,
    val currentStreak: Int,
    val bestStreak: Int,
    val pointsWeek: Int,
)

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ProfileRepository @Inject constructor(
    private val firebase: FirebaseProvider,
    private val auth: dagger.Lazy<AuthRepository>,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) {
    private fun users() = firebase.requireFirestore().collection(USERS)

    fun observeMyProfile(): Flow<UserProfile?> = auth.get().currentUser.flatMapLatest { user ->
        if (user == null || firebase.firestore == null) flowOf(null)
        else users().document(user.uid).observe().map { it.toProfile() }.catch { emit(null) }
    }

    fun observeProfile(uid: String): Flow<UserProfile?> =
        if (firebase.firestore == null) flowOf(null)
        else users().document(uid).observe().map { it.toProfile() }.catch { emit(null) }

    /** Creates the profile on first sign-in (username + invite code), fills gaps on later ones. */
    suspend fun ensureProfile(user: CloudUser) {
        val localName = settings.current().displayName
        val name = user.name.ifBlank { localName }.ifBlank { "Rave friend" }
        if (localName.isBlank()) settings.update { it.copy(displayName = name) }

        val ref = users().document(user.uid)
        val existing = ref.get().await()
        if (!existing.exists()) {
            val username = uniqueUsername(name)
            ref.set(
                mapOf(
                    "name" to name,
                    "nameLower" to name.lowercase(),
                    "username" to username,
                    "usernameLower" to username.lowercase(),
                    "visibility" to settings.current().visibility.name,
                    "photoUrl" to user.photoUrl,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
        if (existing.getString("inviteCode").isNullOrBlank()) {
            runCatching {
                val code = createInviteCode(user.uid, name)
                ref.set(mapOf("inviteCode" to code), SetOptions.merge()).await()
            }
        }
    }

    /** Reserves `invites/{code}` for this user's app invite; retries on (rare) collisions. */
    private suspend fun createInviteCode(uid: String, name: String): String {
        val db = firebase.requireFirestore()
        repeat(6) {
            val code = InviteCodes.generate(name)
            val ref = db.collection(INVITES).document(code)
            val created = db.runTransaction { tx ->
                if (tx.get(ref).exists()) false
                else {
                    tx.set(ref, mapOf("type" to "APP", "targetId" to uid, "createdBy" to uid, "createdByName" to name,
                        "createdAt" to FieldValue.serverTimestamp()))
                    true
                }
            }.await()
            if (created) return code
        }
        error("Could not allocate an invite code")
    }

    private suspend fun uniqueUsername(name: String): String {
        var base = name.lowercase().filter { it.isLetterOrDigit() || it == '_' }.take(14)
        if (base.length < 3) base = (base + "user").take(14)
        if (base.length < 3) base = "rave"
        var candidate = base
        repeat(8) {
            val taken = runCatching { isUsernameTaken(candidate) }.getOrDefault(false)
            if (!taken) return candidate
            val suffix = Random.nextInt(100, 9999).toString()
            candidate = base.take(20 - suffix.length) + suffix
        }
        val suffix = (System.currentTimeMillis() % 100000).toString()
        return base.take(20 - suffix.length) + suffix
    }

    suspend fun isUsernameTaken(username: String, exceptUid: String? = null): Boolean {
        val snap = users().whereEqualTo("usernameLower", username.lowercase()).limit(2).get().await()
        return snap.documents.any { it.id != exceptUid }
    }

    suspend fun updateProfile(name: String, username: String): Result<Unit> = runCatching {
        val uid = firebase.requireUid()
        val clean = username.trim().lowercase()
        require(USERNAME.matches(clean)) { "invalid_username" }
        require(!isUsernameTaken(clean, exceptUid = uid)) { "username_taken" }
        users().document(uid).set(
            mapOf("name" to name.trim(), "nameLower" to name.trim().lowercase(), "username" to clean, "usernameLower" to clean),
            SetOptions.merge(),
        ).await()
        settings.update { it.copy(displayName = name.trim()) }
    }

    suspend fun setVisibility(visibility: ProfileVisibility) {
        val uid = firebase.uid ?: return
        runCatching { users().document(uid).set(mapOf("visibility" to visibility.name), SetOptions.merge()).await() }
    }

    suspend fun pushStats(s: StatsSummary) {
        val uid = firebase.uid ?: return
        users().document(uid).set(
            mapOf(
                "totalKm" to s.totalKm, "weekKm" to s.weekKm, "weekStart" to s.weekStartEpochDay,
                "currentStreak" to s.currentStreak, "bestStreak" to s.bestStreak, "pointsWeek" to s.pointsWeek,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun setFields(fields: Map<String, Any?>) {
        val uid = firebase.uid ?: return
        users().document(uid).set(fields, SetOptions.merge()).await()
    }

    suspend fun registerToken(token: String) {
        val uid = firebase.uid ?: return
        runCatching {
            users().document(uid).collection("tokens").document(token)
                .set(mapOf("createdAt" to FieldValue.serverTimestamp(), "platform" to "android")).await()
        }
    }

    /** Opt-in contact matching: only a salted hash of the user's own number is stored. */
    suspend fun setPhoneHash(hash: String?) = setFields(mapOf("phoneHash" to hash))

    suspend fun getProfile(uid: String): UserProfile? = runCatching { users().document(uid).get().await().toProfile() }.getOrNull()

    suspend fun getProfiles(uids: List<String>): List<UserProfile> {
        if (uids.isEmpty() || firebase.firestore == null) return emptyList()
        return uids.distinct().firestoreChunks().flatMap { chunk ->
            users().whereIn(FieldPath.documentId(), chunk).get().await().documents.mapNotNull { it.toProfile() }
        }
    }

    suspend fun searchByUsername(query: String): List<UserProfile> {
        val q = query.trim().lowercase().removePrefix("@")
        if (q.length < 2) return emptyList()
        val me = firebase.uid
        return users().orderBy("usernameLower").startAt(q).endAt(q + "").limit(20).get().await()
            .documents.mapNotNull { it.toProfile() }
            .filter { it.uid != me && it.visibility != ProfileVisibility.PRIVATE }
    }

    suspend fun findByPhoneHashes(hashes: List<String>): List<UserProfile> {
        val me = firebase.uid
        return hashes.distinct().firestoreChunks().flatMap { chunk ->
            users().whereIn("phoneHash", chunk).get().await().documents.mapNotNull { it.toProfile() }
        }.filter { it.uid != me }
    }

    /** Best-effort removal of everything this user owns in Firestore. */
    suspend fun deleteCloudData(uid: String) {
        val db = firebase.requireFirestore()
        val userRef = users().document(uid)
        for (sub in listOf("friends", "tokens", "activities", "days", "backup")) {
            runCatching {
                userRef.collection(sub).get().await().documents.chunked(400).forEach { batch ->
                    db.runBatch { b -> batch.forEach { b.delete(it.reference) } }.await()
                }
            }
        }
        runCatching {
            db.collection("feed").whereEqualTo("ownerUid", uid).get().await().documents.forEach { it.reference.delete().await() }
        }
        runCatching { userRef.delete().await() }
    }

    companion object {
        const val USERS = "users"
        const val INVITES = "invites"
        val USERNAME = Regex("^[a-z0-9_]{3,20}$")
    }
}

fun DocumentSnapshot.toProfile(): UserProfile? {
    if (!exists()) return null
    return UserProfile(
        uid = id,
        name = str("name"),
        username = str("username"),
        inviteCode = str("inviteCode"),
        visibility = runCatching { ProfileVisibility.valueOf(str("visibility")) }.getOrDefault(ProfileVisibility.FRIENDS),
        photoUrl = getString("photoUrl"),
        totalKm = double("totalKm"),
        weekKm = double("weekKm"),
        weekStartEpochDay = long("weekStart"),
        currentStreak = int("currentStreak"),
        bestStreak = int("bestStreak"),
        pointsWeek = int("pointsWeek"),
        invitedBy = getString("invitedBy"),
    )
}
