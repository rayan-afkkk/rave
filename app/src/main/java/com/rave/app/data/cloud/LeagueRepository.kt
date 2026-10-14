package com.rave.app.data.cloud

import com.google.firebase.firestore.FieldValue
import com.rave.app.core.TimeProvider
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.StepRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.temporal.IsoFields
import javax.inject.Inject
import javax.inject.Singleton

data class LeagueEntry(val uid: String, val name: String, val steps: Int, val isMe: Boolean)

data class LeagueStandings(val week: String, val entries: List<LeagueEntry>, val daysLeft: Int, val hasFriends: Boolean)

/**
 * Weekly step league between you and your friends (Mon-Sun, resets every Monday).
 * Each user publishes their own total to `weeklySteps/{week}_{uid}`; friends can read it
 * (see firestore.rules). Small friend groups competing on a weekly leaderboard is the
 * format with the strongest evidence for lasting activity gains.
 */
@Singleton
class LeagueRepository @Inject constructor(
    private val firebase: FirebaseProvider,
    private val friends: FriendsRepository,
    private val steps: StepRepository,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) {
    fun weekId(date: LocalDate = time.today()): String =
        "%d-W%02d".format(date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))

    private fun col() = firebase.requireFirestore().collection(COLLECTION)

    /** Uploads this week's step total if it changed. */
    suspend fun publish() {
        val uid = firebase.uid ?: return
        val today = time.today()
        val week = weekId(today)
        val total = steps.stepsBetween(time.weekStart(today), today)
        val key = "league_published"
        val stamp = "$week:$total"
        if (settings.getString(key) == stamp) return
        col().document("${week}_$uid").set(
            mapOf(
                "uid" to uid, "week" to week, "steps" to total,
                "name" to settings.current().displayName.ifBlank { "Rave friend" },
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        settings.putString(key, stamp)
    }

    suspend fun standings(): LeagueStandings {
        val uid = firebase.requireUid()
        val today = time.today()
        val week = weekId(today)
        runCatching { publish() }
        val friendList = friends.observeFriends().first()
        val myName = settings.current().displayName.ifBlank { "You" }
        val mySteps = steps.stepsBetween(time.weekStart(today), today)
        val others = coroutineScope {
            friendList.map { f ->
                async {
                    val doc = runCatching { col().document("${week}_${f.uid}").get().await() }.getOrNull()
                    LeagueEntry(f.uid, f.name, doc?.getLong("steps")?.toInt() ?: 0, isMe = false)
                }
            }.map { it.await() }
        }
        val entries = (others + LeagueEntry(uid, myName, mySteps, isMe = true))
            .sortedWith(compareByDescending<LeagueEntry> { it.steps }.thenBy { !it.isMe })
        val daysLeft = (7 - today.dayOfWeek.value).coerceAtLeast(0)
        return LeagueStandings(week, entries, daysLeft, friendList.isNotEmpty())
    }

    companion object {
        const val COLLECTION = "weeklySteps"
    }
}
