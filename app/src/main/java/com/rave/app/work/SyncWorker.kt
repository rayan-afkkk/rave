package com.rave.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.rave.app.BuildConfig
import com.rave.app.R
import com.rave.app.core.TimeProvider
import com.rave.app.data.cloud.CompetitionRepository
import com.rave.app.data.cloud.FeedRepository
import com.rave.app.data.cloud.FirebaseProvider
import com.rave.app.data.cloud.FriendsRepository
import com.rave.app.data.cloud.GuestMigration
import com.rave.app.data.cloud.ProfileRepository
import com.rave.app.data.cloud.SharedStreakRepository
import com.rave.app.data.cloud.StatsSummary
import com.rave.app.data.local.dao.ActivityDao
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.ActivityRepository
import com.rave.app.data.repository.HabitRepository
import com.rave.app.data.repository.PrivacyZoneRepository
import com.rave.app.data.repository.StatsRepository
import com.rave.app.data.repository.StreakRepository
import com.rave.app.domain.competition.CompetitionRanking
import com.rave.app.domain.competition.CompetitionStatus
import com.rave.app.domain.model.ProfileVisibility
import com.rave.app.notifications.Notifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/**
 * Cloud sync for signed-in users (no-op for guests or without Firebase):
 * activity summaries + privacy-trimmed feed posts, daily habit summaries, profile stats,
 * competition scores, invite rewards — and local notifications for competition changes and
 * friend events (no server code needed).
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val firebase: FirebaseProvider,
    private val activityDao: ActivityDao,
    private val activities: ActivityRepository,
    private val habits: HabitRepository,
    private val privacy: PrivacyZoneRepository,
    private val profiles: ProfileRepository,
    private val friends: FriendsRepository,
    private val feed: FeedRepository,
    private val competitions: CompetitionRepository,
    private val sharedStreaks: SharedStreakRepository,
    private val streaks: StreakRepository,
    private val stats: StatsRepository,
    private val migration: GuestMigration,
    private val settings: SettingsRepository,
    private val notifier: Notifier,
    private val time: TimeProvider,
    private val league: com.rave.app.data.cloud.LeagueRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = firebase.uid ?: return Result.success()
        return try {
            registerToken()
            processPendingDeletes(uid)
            uploadActivities(uid)
            uploadDays(uid)
            pushStats()
            runCatching { league.publish() }
            competitions.refreshAllMyScores()
            watchCompetitions(uid)
            sharedStreaks.cleanUpDeclined()
            notifyFriendEvents()
            applyInviteRewards(uid)
            dailyBackup()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun registerToken() {
        runCatching { profiles.registerToken(FirebaseMessaging.getInstance().token.await()) }
    }

    private suspend fun processPendingDeletes(uid: String) {
        val ids = settings.getString(PENDING_DELETES)?.split(',')?.filter { it.isNotBlank() }.orEmpty()
        if (ids.isEmpty()) return
        val userActivities = firebase.requireFirestore().collection(ProfileRepository.USERS).document(uid).collection("activities")
        ids.forEach { id ->
            runCatching { userActivities.document(id).delete().await() }
            feed.remove(id)
        }
        settings.putString(PENDING_DELETES, null)
    }

    private suspend fun uploadActivities(uid: String) {
        val unsynced = activityDao.getUnsynced()
        if (unsynced.isEmpty()) return
        val prefs = settings.current()
        val friendIds = friends.observeFriendIds().first()
        val userActivities = firebase.requireFirestore().collection(ProfileRepository.USERS).document(uid).collection("activities")
        for (a in unsynced) {
            val remoteId = a.remoteId ?: "${uid}_${a.id}_${a.startTime}"
            userActivities.document(remoteId).set(
                mapOf(
                    "type" to a.type.name, "name" to a.name, "startTime" to a.startTime, "endTime" to a.endTime,
                    "distanceM" to a.distanceM, "durationS" to a.durationS, "movingTimeS" to a.movingTimeS,
                    "autoDetected" to a.autoDetected, "gpsTracked" to true, "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
            if (prefs.visibility == ProfileVisibility.FRIENDS) {
                val segments = privacy.publicSegments(activities.points(a.id))
                feed.publish(remoteId, a, prefs.displayName, segments, friendIds)
            }
            activityDao.markSynced(a.id, remoteId)
        }
        // Invitee half of the invite reward: mark the first activity on our profile.
        profiles.setFields(mapOf("firstActivityAt" to (activityDao.getAllCompleted().minOfOrNull { it.startTime })))
    }

    /** Per-day habit summaries (used by habit and active-day competitions). */
    private suspend fun uploadDays(uid: String) {
        val today = time.today()
        val from = today.minusDays(7)
        val names = habits.observeAllHabitsOnce().associate { it.id to it.name }
        val logs = habits.logsBetween(from, today).filter { it.completed }.groupBy { it.date }
        val days = firebase.requireFirestore().collection(ProfileRepository.USERS).document(uid).collection("days")
        var d = from
        while (!d.isAfter(today)) {
            val done = logs[d].orEmpty().mapNotNull { names[it.habitId] }
            days.document(d.toEpochDay().toString()).set(mapOf("habitsCompleted" to done, "epochDay" to d.toEpochDay())).await()
            d = d.plusDays(1)
        }
    }

    private suspend fun pushStats() {
        val s = stats.observeProfileStats().first()
        profiles.pushStats(
            StatsSummary(
                totalKm = s.totalDistanceM / 1000.0,
                weekKm = s.weekDistanceM / 1000.0,
                weekStartEpochDay = time.weekStart().toEpochDay(),
                currentStreak = s.currentStreak,
                bestStreak = s.bestStreak,
                pointsWeek = s.pointsThisWeek,
            ),
        )
    }

    /** Local notifications: someone overtook you, 24 h left, competition ended (+ win badge). */
    private suspend fun watchCompetitions(uid: String) {
        val now = time.nowMs()
        val mine = competitions.observeMine().first()
        for (c in mine) {
            val status = c.statusAt(now)
            if (status == CompetitionStatus.UPCOMING) continue
            val rows = competitions.participants(c.id)
            val ranked = CompetitionRanking.rank(rows.map { it.score }, c.type)
            val rankKey = "comp_ranks_${c.id}"
            val previous = settings.getString(rankKey)?.let { json ->
                runCatching { JSONObject(json).let { o -> o.keys().asSequence().associateWith { o.getInt(it) } } }.getOrNull()
            }.orEmpty()

            if (status == CompetitionStatus.ACTIVE && previous.isNotEmpty()) {
                val overtakers = CompetitionRanking.overtakers(previous, ranked, uid)
                overtakers.firstOrNull()?.let { who ->
                    val name = ranked.first { it.score.uid == who }.score.name
                    notifier.competition(c.id.hashCode(), applicationContext.getString(R.string.notif_overtaken_title, name),
                        applicationContext.getString(R.string.notif_overtaken_body, c.name), c.id)
                }
            }
            if (status == CompetitionStatus.ACTIVE && c.endAtMs - now < DAY_MS && settings.getString("comp_24h_${c.id}") == null) {
                settings.putString("comp_24h_${c.id}", "1")
                notifier.competition(c.id.hashCode() + 1, applicationContext.getString(R.string.notif_24h_title, c.name),
                    applicationContext.getString(R.string.notif_24h_body), c.id)
            }
            if (status == CompetitionStatus.ENDED && settings.getString("comp_end_${c.id}") == null) {
                settings.putString("comp_end_${c.id}", "1")
                val myRank = ranked.firstOrNull { it.score.uid == uid }?.rank
                if (myRank == 1) stats.recordCompetitionWin(c.id, c.name)
                notifier.competition(c.id.hashCode() + 2, applicationContext.getString(R.string.notif_ended_title, c.name),
                    if (myRank == 1) applicationContext.getString(R.string.notif_ended_won)
                    else applicationContext.getString(R.string.notif_ended_body, myRank ?: 0), c.id)
            }
            settings.putString(rankKey, JSONObject(ranked.associate { it.score.uid to it.rank }).toString())
        }
    }

    /** Friend requests, invitations and nudges seen since the last run. */
    private suspend fun notifyFriendEvents() {
        val lastCheck = settings.getString(LAST_SOCIAL_CHECK)?.toLongOrNull() ?: time.nowMs()
        val ctx = applicationContext
        val requests = friends.observeRequests().first().incoming
        val seen = settings.getString(SEEN_REQUESTS)?.split(',').orEmpty().toSet()
        requests.filter { it.id !in seen }.forEach {
            notifier.social(it.id.hashCode(), ctx.getString(R.string.notif_friend_request_title), ctx.getString(R.string.notif_friend_request_body, it.fromName))
        }
        settings.putString(SEEN_REQUESTS, requests.joinToString(",") { it.id })

        val seenComps = settings.getString(SEEN_COMP_INVITES)?.split(',').orEmpty().toSet()
        val invites = competitions.observeInvitations().first()
        invites.filter { it.id !in seenComps }.forEach {
            notifier.competition(it.id.hashCode() + 3, ctx.getString(R.string.notif_comp_invite_title), ctx.getString(R.string.notif_comp_invite_body, it.name), it.id)
        }
        settings.putString(SEEN_COMP_INVITES, invites.joinToString(",") { it.id })

        friends.nudgesSince(lastCheck).forEachIndexed { i, (from, _) ->
            notifier.social(i + 77, ctx.getString(R.string.notif_nudge_title, from), ctx.getString(R.string.notif_nudge_body))
        }
        settings.putString(LAST_SOCIAL_CHECK, time.nowMs().toString())
    }

    /** Both inviter and invitee get one streak freeze when the invitee completes a first activity. */
    private suspend fun applyInviteRewards(uid: String) {
        if (!BuildConfig.INVITE_REWARD_ENABLED) return
        // Invitee side.
        val me = profiles.getProfile(uid)
        val local = stats.userStats()
        if (me?.invitedBy != null && !local.inviteRewardClaimed && activityDao.getAllCompleted().isNotEmpty()) {
            stats.updateUserStats { it.copy(inviteRewardClaimed = true, invitedByUid = me.invitedBy) }
            streaks.grantBonusFreeze()
        }
        // Inviter side: friends who joined with our code and finished a first activity.
        val rewarded = settings.getString(REWARDED_INVITEES)?.split(',').orEmpty().toMutableSet()
        val invitees = runCatching {
            firebase.requireFirestore().collection(ProfileRepository.USERS).whereEqualTo("invitedBy", uid).get().await().documents
        }.getOrDefault(emptyList())
        var changed = false
        for (doc in invitees) {
            if (doc.id in rewarded || doc.get("firstActivityAt") == null) continue
            rewarded += doc.id
            changed = true
            stats.incrementInvitesAccepted()
            streaks.grantBonusFreeze()
        }
        if (changed) settings.putString(REWARDED_INVITEES, rewarded.joinToString(","))
    }

    private suspend fun dailyBackup() {
        val today = time.today().toEpochDay().toString()
        if (settings.getString(LAST_BACKUP) == today) return
        runCatching { migration.uploadBackup() }.onSuccess { settings.putString(LAST_BACKUP, today) }
    }

    companion object {
        const val PENDING_DELETES = "pending_cloud_deletes"
        private const val LAST_SOCIAL_CHECK = "last_social_check"
        private const val SEEN_REQUESTS = "seen_requests"
        private const val SEEN_COMP_INVITES = "seen_comp_invites"
        private const val REWARDED_INVITEES = "rewarded_invitees"
        private const val LAST_BACKUP = "last_backup"
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
