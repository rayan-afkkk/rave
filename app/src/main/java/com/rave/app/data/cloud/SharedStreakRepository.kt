package com.rave.app.data.cloud

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.rave.app.core.TimeProvider
import com.rave.app.data.local.dao.HabitDao
import com.rave.app.data.local.toDomain
import com.rave.app.data.local.toEntity
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.HabitCompletionListener
import com.rave.app.domain.model.Habit
import com.rave.app.domain.model.HabitColor
import com.rave.app.domain.model.HabitType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

enum class SharedStatus { PENDING, ACTIVE, DECLINED }

data class SharedStreak(
    val id: String,
    val members: List<String>,
    val memberNames: Map<String, String>,
    val habitName: String,
    val icon: String,
    val color: HabitColor,
    val type: HabitType,
    val target: Int,
    val status: SharedStatus,
    val invitedBy: String,
    /** Streak as it stands today (0 if both missed a day). */
    val current: Int,
    val best: Int,
    /** uid → done today. */
    val today: Map<String, Boolean>,
) {
    fun friendUid(me: String): String = members.firstOrNull { it != me } ?: ""
    fun friendName(me: String): String = memberNames[friendUid(me)].orEmpty()
    fun doneToday(uid: String): Boolean = today[uid] == true
    val bothDoneToday: Boolean get() = members.size == 2 && members.all { today[it] == true }
}

/**
 * Shared streaks (`sharedStreaks/{id}`): two friends attach the same habit; the streak only
 * grows on days both complete it. Completion updates run in a Firestore transaction.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SharedStreakRepository @Inject constructor(
    private val firebase: FirebaseProvider,
    private val auth: AuthRepository,
    private val habitDao: HabitDao,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) : HabitCompletionListener {

    private fun col() = firebase.requireFirestore().collection(COLLECTION)

    private fun observeMine(): Flow<List<SharedStreak>> = auth.currentUser.flatMapLatest { user ->
        if (user == null || firebase.firestore == null) flowOf(emptyList())
        else col().whereArrayContains("members", user.uid).observe()
            .map { s -> s.documents.mapNotNull { it.toShared() } }
            .catch { emit(emptyList()) }
    }

    fun observeActive(): Flow<List<SharedStreak>> = observeMine().map { l -> l.filter { it.status == SharedStatus.ACTIVE } }

    fun observeInvitations(): Flow<List<SharedStreak>> = observeMine().map { l ->
        val me = firebase.uid
        l.filter { it.status == SharedStatus.PENDING && it.invitedBy != me }
    }

    fun observePendingSent(): Flow<List<SharedStreak>> = observeMine().map { l ->
        val me = firebase.uid
        l.filter { it.status == SharedStatus.PENDING && it.invitedBy == me }
    }

    /** Invites [friendUid] to share [habit]; links the local habit to the new shared streak. */
    suspend fun invite(friendUid: String, friendName: String, habit: Habit): Result<String> = runCatching {
        val me = firebase.requireUid()
        val myName = settings.current().displayName
        val ref = col().document()
        ref.set(
            mapOf(
                "members" to listOf(me, friendUid),
                "memberNames" to mapOf(me to myName, friendUid to friendName),
                "habitName" to habit.name, "icon" to habit.icon, "color" to habit.color.name,
                "type" to habit.type.name, "target" to habit.target,
                "status" to SharedStatus.PENDING.name, "invitedBy" to me,
                "current" to 0, "best" to 0, "lastBothDate" to null, "days" to emptyMap<String, Any>(),
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        linkLocal(habit.id, ref.id, friendName)
        ref.id
    }

    /** Accepts an invitation and creates the matching habit locally. */
    suspend fun accept(id: String): Result<Unit> = runCatching {
        val me = firebase.requireUid()
        val doc = col().document(id).get().await()
        val shared = doc.toShared() ?: error("not_found")
        require(me in shared.members) { "not_member" }
        col().document(id).update("status", SharedStatus.ACTIVE.name).await()
        if (habitDao.getBySharedStreak(id) == null) {
            val habit = Habit(
                name = shared.habitName, icon = shared.icon, color = shared.color,
                type = if (shared.type == HabitType.ACTIVITY || shared.type == HabitType.WORKOUT) HabitType.CHECKBOX else shared.type,
                target = shared.target, sharedStreakId = id, sharedWithName = shared.friendName(me),
                createdDate = time.today(),
            )
            habitDao.upsert(habit.toEntity(createdAt = time.nowMs(), sortOrder = habitDao.count()))
        }
    }

    suspend fun decline(id: String): Result<Unit> = runCatching {
        col().document(id).update("status", SharedStatus.DECLINED.name).await()
    }

    /** Unlinks local habits whose invitation was declined (called by the sync worker). */
    suspend fun cleanUpDeclined() {
        val me = firebase.uid ?: return
        val docs = runCatching { col().whereArrayContains("members", me).get().await().documents }.getOrNull() ?: return
        docs.mapNotNull { it.toShared() }.filter { it.status == SharedStatus.DECLINED }.forEach { s ->
            habitDao.getBySharedStreak(s.id)?.let { habitDao.upsert(it.copy(sharedStreakId = null, sharedWithName = null)) }
        }
    }

    override suspend fun onHabitCompletionChanged(habit: Habit, date: LocalDate, completed: Boolean) {
        val id = habit.sharedStreakId ?: return
        if (firebase.uid == null) return
        runCatching { markCompletion(id, date, completed) }
    }

    /**
     * Records my completion for [date]. When both members are done, the streak increments
     * (continuing from yesterday, or restarting at 1). Undoing reverses today's increment.
     */
    suspend fun markCompletion(id: String, date: LocalDate, done: Boolean) {
        val me = firebase.requireUid()
        val db = firebase.requireFirestore()
        val ref = col().document(id)
        val day = date.toEpochDay()
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val members = snap.stringList("members")
            if (me !in members) return@runTransaction null
            @Suppress("UNCHECKED_CAST")
            val days = (snap.get("days") as? Map<String, Map<String, Boolean>>).orEmpty().toMutableMap()
            val todayMap = days[day.toString()].orEmpty().toMutableMap()
            val wasBoth = members.all { todayMap[it] == true }
            todayMap[me] = done
            days[day.toString()] = todayMap
            // Keep only the last 14 days to bound document size.
            val trimmed = days.filterKeys { (it.toLongOrNull() ?: 0) >= day - 14 }
            val isBoth = members.all { todayMap[it] == true }

            var current = snap.int("current")
            var best = snap.int("best")
            var lastBoth = snap.getLong("lastBothDate")
            if (isBoth && !wasBoth) {
                current = if (lastBoth == day - 1) current + 1 else 1
                best = maxOf(best, current)
                lastBoth = day
            } else if (!isBoth && wasBoth && lastBoth == day) {
                current = (current - 1).coerceAtLeast(0)
                lastBoth = if (current > 0) day - 1 else null
            }
            tx.update(ref, mapOf("days" to trimmed, "current" to current, "best" to best, "lastBothDate" to lastBoth))
            null
        }.await()
    }

    private suspend fun linkLocal(habitId: Long, sharedId: String, friendName: String) {
        habitDao.getById(habitId)?.let { habitDao.upsert(it.copy(sharedStreakId = sharedId, sharedWithName = friendName)) }
    }

    private fun DocumentSnapshot.toShared(): SharedStreak? {
        if (!exists()) return null
        val today = time.today().toEpochDay()
        val lastBoth = getLong("lastBothDate")
        @Suppress("UNCHECKED_CAST")
        val days = (get("days") as? Map<String, Map<String, Boolean>>).orEmpty()
        val storedCurrent = int("current")
        return SharedStreak(
            id = id,
            members = stringList("members"),
            memberNames = map("memberNames").mapValues { it.value as? String ?: "" },
            habitName = str("habitName"),
            icon = getString("icon") ?: "check",
            color = runCatching { HabitColor.valueOf(str("color")) }.getOrDefault(HabitColor.PEACH),
            type = runCatching { HabitType.valueOf(str("type")) }.getOrDefault(HabitType.CHECKBOX),
            target = int("target").coerceAtLeast(1),
            status = runCatching { SharedStatus.valueOf(str("status")) }.getOrDefault(SharedStatus.PENDING),
            invitedBy = str("invitedBy"),
            current = if (lastBoth != null && lastBoth >= today - 1) storedCurrent else 0,
            best = int("best"),
            today = days[today.toString()].orEmpty(),
        )
    }

    suspend fun localHabitFor(sharedId: String): Habit? = habitDao.getBySharedStreak(sharedId)?.toDomain()

    companion object {
        const val COLLECTION = "sharedStreaks"
    }
}
