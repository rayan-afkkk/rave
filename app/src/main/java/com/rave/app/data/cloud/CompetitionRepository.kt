package com.rave.app.data.cloud

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.rave.app.core.TimeProvider
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.ActivityRepository
import com.rave.app.data.repository.HabitRepository
import com.rave.app.domain.competition.Competition
import com.rave.app.domain.competition.CompetitionMode
import com.rave.app.domain.competition.CompetitionScoring
import com.rave.app.domain.competition.CompetitionStatus
import com.rave.app.domain.competition.CompetitionType
import com.rave.app.domain.competition.HabitDay
import com.rave.app.domain.competition.ParticipantScore
import com.rave.app.domain.model.ActivityType
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

data class CompetitionDraft(
    val name: String,
    val type: CompetitionType,
    val mode: CompetitionMode,
    val activityFilter: ActivityType?,
    val targetKm: Double,
    val habitNames: List<String>,
    val startAtMs: Long,
    val endAtMs: Long,
    val stake: String?,
    val invited: List<String>,
    val teamNames: List<String>,
)

/** A participant row plus their cumulative value per day (for the progress chart). */
data class ParticipantRow(val score: ParticipantScore, val daily: Map<Long, Double>)

/**
 * `competitions/{id}` + `competitions/{id}/participants/{uid}`. Each participant writes only
 * their own doc with a value computed from their own synced activities / habit logs inside the
 * window (security rules stop anyone writing someone else's score).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CompetitionRepository @Inject constructor(
    private val firebase: FirebaseProvider,
    private val auth: AuthRepository,
    private val settings: SettingsRepository,
    private val activities: ActivityRepository,
    private val habits: HabitRepository,
    private val time: TimeProvider,
) {
    private fun col() = firebase.requireFirestore().collection(COLLECTION)

    fun observeMine(): Flow<List<Competition>> = auth.currentUser.flatMapLatest { user ->
        if (user == null || firebase.firestore == null) flowOf(emptyList())
        else col().whereArrayContains("members", user.uid).observe()
            .map { s -> s.documents.mapNotNull { it.toCompetition() }.sortedByDescending { it.endAtMs } }
            .catch { emit(emptyList()) }
    }

    fun observeInvitations(): Flow<List<Competition>> = auth.currentUser.flatMapLatest { user ->
        if (user == null || firebase.firestore == null) flowOf(emptyList())
        else col().whereArrayContains("invited", user.uid).observe()
            .map { s -> s.documents.mapNotNull { it.toCompetition() }.filter { user.uid !in it.members && it.statusAt(time.nowMs()) != CompetitionStatus.ENDED } }
            .catch { emit(emptyList()) }
    }

    fun observe(id: String): Flow<Competition?> =
        if (firebase.firestore == null) flowOf(null)
        else col().document(id).observe().map { it.toCompetition() }.catch { emit(null) }

    fun observeParticipants(id: String): Flow<List<ParticipantRow>> =
        if (firebase.firestore == null) flowOf(emptyList())
        else col().document(id).collection(PARTICIPANTS).observe()
            .map { s -> s.documents.map { it.toParticipant() } }
            .catch { emit(emptyList()) }

    suspend fun get(id: String): Competition? = runCatching { col().document(id).get().await().toCompetition() }.getOrNull()

    suspend fun participants(id: String): List<ParticipantRow> =
        runCatching { col().document(id).collection(PARTICIPANTS).get().await().documents.map { it.toParticipant() } }.getOrDefault(emptyList())

    suspend fun create(d: CompetitionDraft): Result<String> = runCatching {
        val me = firebase.requireUid()
        val myName = settings.current().displayName
        require(d.invited.size + 1 <= Competition.MAX_PARTICIPANTS) { "too_many" }
        val ref = col().document()
        ref.set(
            mapOf(
                "name" to d.name.trim(), "type" to d.type.name, "mode" to d.mode.name,
                "activityFilter" to d.activityFilter?.name, "targetKm" to d.targetKm, "habitNames" to d.habitNames,
                "startAt" to d.startAtMs, "endAt" to d.endAtMs, "stake" to d.stake?.trim()?.ifEmpty { null },
                "createdBy" to me, "createdByName" to myName,
                "members" to listOf(me), "invited" to d.invited.distinct(), "teams" to mapOf(me to 0),
                "teamNames" to d.teamNames, "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        writeParticipant(ref.id, me, myName, team = if (d.mode == CompetitionMode.TEAMS) 0 else null)
        ref.id
    }

    /** Joins via invitation or link; TEAMS mode puts you on the smaller team. */
    suspend fun join(id: String): Result<Unit> = runCatching {
        val me = firebase.requireUid()
        val db = firebase.requireFirestore()
        val ref = col().document(id)
        var team: Int? = null
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val c = snap.toCompetition() ?: error("not_found")
            if (me in c.members) return@runTransaction null
            require(c.members.size < Competition.MAX_PARTICIPANTS) { "full" }
            require(c.statusAt(time.nowMs()) != CompetitionStatus.ENDED) { "ended" }
            val updates = mutableMapOf<String, Any?>(
                "members" to FieldValue.arrayUnion(me),
                "invited" to FieldValue.arrayRemove(me),
            )
            if (c.mode == CompetitionMode.TEAMS) {
                @Suppress("UNCHECKED_CAST")
                val teams = (snap.get("teams") as? Map<String, Number>).orEmpty()
                val zero = teams.values.count { it.toInt() == 0 }
                team = if (zero <= teams.size - zero) 0 else 1
                updates["teams.$me"] = team
            }
            tx.update(ref, updates)
            null
        }.await()
        writeParticipant(id, me, settings.current().displayName, team)
        refreshMyScore(id)
    }

    suspend fun decline(id: String): Result<Unit> = runCatching {
        col().document(id).update("invited", FieldValue.arrayRemove(firebase.requireUid())).await()
    }

    suspend fun inviteMore(id: String, uids: List<String>): Result<Unit> = runCatching {
        col().document(id).update("invited", FieldValue.arrayUnion(*uids.toTypedArray())).await()
    }

    private suspend fun writeParticipant(id: String, uid: String, name: String, team: Int?) {
        col().document(id).collection(PARTICIPANTS).document(uid).set(
            mapOf("name" to name, "team" to team, "value" to 0.0, "lastUpdated" to time.nowMs()),
            SetOptions.merge(),
        ).await()
    }

    /** Recomputes my value for every competition I'm in that is running (or just ended). */
    suspend fun refreshAllMyScores() {
        val me = firebase.uid ?: return
        val mine = runCatching { col().whereArrayContains("members", me).get().await().documents.mapNotNull { it.toCompetition() } }
            .getOrDefault(emptyList())
        val now = time.nowMs()
        mine.filter { now >= it.startAtMs && now < it.endAtMs + GRACE_MS }.forEach { runCatching { refreshMyScore(it.id) } }
    }

    suspend fun refreshMyScore(id: String) {
        val me = firebase.requireUid()
        val c = get(id) ?: return
        val startDate = time.dateOf(c.startAtMs)
        val endDate = minOf(time.dateOf(c.endAtMs - 1), time.today())
        if (endDate.isBefore(startDate)) return
        val summaries = activities.summariesBetween(c.startAtMs, c.endAtMs)
        val habitDays = habitDays(startDate, endDate)
        val result = CompetitionScoring.score(c, summaries, habitDays, startDate, endDate)
        // Cumulative value per day for the daily progress chart.
        val daily = mutableMapOf<String, Double>()
        var d = startDate
        while (!d.isAfter(endDate)) {
            val partial = CompetitionScoring.score(c, summaries.filter { time.dateOf(it.startTimeMs) <= d }, habitDays, startDate, d)
            daily[d.toEpochDay().toString()] = partial.value
            d = d.plusDays(1)
        }
        col().document(id).collection(PARTICIPANTS).document(me).set(
            mapOf(
                "name" to settings.current().displayName,
                "value" to result.value,
                "reachedTargetAt" to result.reachedTargetAtMs,
                "flagged" to result.flagged,
                "daily" to daily,
                "lastUpdated" to time.nowMs(),
            ),
            SetOptions.merge(),
        ).await()
    }

    private suspend fun habitDays(from: LocalDate, to: LocalDate): List<HabitDay> {
        val names = habits.observeAllHabitsOnce().associate { it.id to it.name }
        return habits.logsBetween(from, to).filter { it.completed }.groupBy { it.date }
            .map { (date, logs) -> HabitDay(date, logs.mapNotNull { names[it.habitId] }) }
    }

    private fun DocumentSnapshot.toCompetition(): Competition? {
        if (!exists()) return null
        return Competition(
            id = id,
            name = str("name"),
            type = runCatching { CompetitionType.valueOf(str("type")) }.getOrDefault(CompetitionType.DISTANCE),
            mode = runCatching { CompetitionMode.valueOf(str("mode")) }.getOrDefault(CompetitionMode.GROUP),
            activityFilter = getString("activityFilter")?.let { runCatching { ActivityType.valueOf(it) }.getOrNull() },
            targetKm = double("targetKm"),
            habitNames = stringList("habitNames"),
            startAtMs = long("startAt"),
            endAtMs = long("endAt"),
            stake = getString("stake"),
            createdBy = str("createdBy"),
            members = stringList("members"),
            invited = stringList("invited"),
            teamNames = stringList("teamNames"),
        )
    }

    private fun DocumentSnapshot.toParticipant(): ParticipantRow {
        val daily = map("daily").mapNotNull { (k, v) -> k.toLongOrNull()?.let { it to ((v as? Number)?.toDouble() ?: 0.0) } }.toMap()
        return ParticipantRow(
            ParticipantScore(
                uid = id,
                name = str("name"),
                value = double("value"),
                team = getLong("team")?.toInt(),
                lastUpdatedMs = long("lastUpdated"),
                reachedTargetAtMs = getLong("reachedTargetAt"),
                flaggedActivities = int("flagged"),
            ),
            daily,
        )
    }

    companion object {
        const val COLLECTION = "competitions"
        const val PARTICIPANTS = "participants"
        /** Keep updating scores for 6 h after the end so late syncs still count. */
        private const val GRACE_MS = 6 * 60 * 60 * 1000L
    }
}
