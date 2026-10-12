package com.rave.app.data.repository

import com.rave.app.core.TimeProvider
import com.rave.app.data.local.dao.ActivityDao
import com.rave.app.data.local.dao.HabitLogDao
import com.rave.app.data.local.dao.StatsDao
import com.rave.app.data.local.entity.BadgeEntity
import com.rave.app.data.local.entity.CompetitionWinEntity
import com.rave.app.data.local.entity.StreakEntity
import com.rave.app.data.local.entity.UserStatsEntity
import com.rave.app.domain.habit.BadgeEvaluator
import com.rave.app.domain.habit.BadgeId
import com.rave.app.domain.habit.BadgeInputs
import com.rave.app.domain.habit.Points
import com.rave.app.domain.model.ActivityType
import com.rave.app.notifications.Notifier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

data class ProfileStats(
    val totalDistanceM: Double = 0.0,
    val weekDistanceM: Double = 0.0,
    val totalActivities: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val pointsThisWeek: Int = 0,
    val pointsTotal: Int = 0,
    val level: Int = 1,
    val levelProgress: Float = 0f,
    val freezes: Int = 0,
    /** Earned badges with the time they were earned. */
    val badges: Map<BadgeId, Long> = emptyMap(),
    val competitionWins: Int = 0,
)

@Singleton
class StatsRepository @Inject constructor(
    private val activityDao: ActivityDao,
    private val logDao: HabitLogDao,
    private val statsDao: StatsDao,
    private val time: TimeProvider,
    private val notifier: Notifier,
    private val workoutDao: com.rave.app.data.local.dao.WorkoutDao,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeProfileStats(): Flow<ProfileStats> = time.todayFlow.flatMapLatest { today ->
        val weekStart = time.weekStart(today)
        val weekStartMs = time.startOfDayMs(weekStart)
        val activitiesAndLogs = combine(
            activityDao.observeCompleted(),
            logDao.observeCompletedCount(),
            logDao.observeCompletedCountBetween(weekStart.toEpochDay(), today.toEpochDay()),
        ) { activities, allDone, weekDone -> Triple(activities, allDone, weekDone) }
        val sets = combine(workoutDao.observeWorkingSetCount(), workoutDao.observeWorkingSetCountSince(weekStartMs)) { all, week -> all to week }
        val streaksAndRest = combine(
            statsDao.observeStreaks(),
            statsDao.observeFreezes(),
            statsDao.observeBadges(),
            statsDao.observeWins(),
        ) { streaks, freezes, badges, wins -> Quad(streaks, freezes?.count ?: 0, badges, wins) }

        combine(activitiesAndLogs, streaksAndRest, sets) { (activities, allDone, weekDone), (streaks, freezes, badges, wins), (setsAll, setsWeek) ->
            val total = activities.sumOf { it.distanceM }
            val week = activities.filter { it.startTime >= weekStartMs }.sumOf { it.distanceM }
            val weekWins = wins.count { it.wonAt >= weekStartMs }
            val overall = streaks.firstOrNull { it.habitId == StreakEntity.OVERALL }
            val pointsTotal = Points.compute(allDone, total, wins.size, setsAll)
            ProfileStats(
                totalDistanceM = total,
                weekDistanceM = week,
                totalActivities = activities.size,
                currentStreak = overall?.current ?: 0,
                bestStreak = streaks.maxOfOrNull { it.best } ?: 0,
                pointsThisWeek = Points.compute(weekDone, week, weekWins, setsWeek),
                pointsTotal = pointsTotal,
                level = Points.level(pointsTotal),
                levelProgress = Points.levelProgress(pointsTotal),
                freezes = freezes,
                badges = badges.mapNotNull { b -> runCatching { BadgeId.valueOf(b.id) }.getOrNull()?.let { it to b.earnedAt } }.toMap(),
                competitionWins = wins.size,
            )
        }
    }

    /** Evaluates badge rules and stores (and announces) newly earned ones. */
    suspend fun checkBadges(): List<BadgeId> {
        val activities = activityDao.getAllCompleted()
        val streaks = statsDao.getStreaks()
        val userStats = statsDao.getUserStats() ?: UserStatsEntity()
        val inputs = BadgeInputs(
            totalActivities = activities.size,
            longestActivityM = activities.maxOfOrNull { it.distanceM } ?: 0.0,
            longestRunM = activities.filter { it.type == ActivityType.RUN }.maxOfOrNull { it.distanceM } ?: 0.0,
            totalDistanceM = activities.sumOf { it.distanceM },
            bestStreak = streaks.maxOfOrNull { it.best } ?: 0,
            earlyActivities = activities.count { time.dateTimeOf(it.startTime).hour < 7 },
            totalHabitCompletions = logDao.observeCompletedCount().first(),
            competitionWins = statsDao.observeWins().first().size,
            invitesAccepted = userStats.invitesAccepted,
            workouts = workoutDao.finishedWorkouts().size,
            workingSets = workoutDao.allWorkingSets().size,
        )
        val already = statsDao.getBadges().map { it.id }.toSet()
        val newOnes = BadgeEvaluator.earned(inputs).filter { it.name !in already }
        if (newOnes.isNotEmpty()) {
            statsDao.insertBadges(newOnes.map { BadgeEntity(it.name, time.nowMs()) })
            newOnes.forEach { notifier.badgeEarned(it) }
        }
        return newOnes
    }

    suspend fun recordCompetitionWin(competitionId: String, name: String): Boolean {
        val inserted = statsDao.insertWin(CompetitionWinEntity(competitionId, name, time.nowMs())) != -1L
        if (inserted) checkBadges()
        return inserted
    }

    suspend fun userStats(): UserStatsEntity = statsDao.getUserStats() ?: UserStatsEntity()

    suspend fun updateUserStats(transform: (UserStatsEntity) -> UserStatsEntity) {
        statsDao.upsertUserStats(transform(userStats()))
    }

    suspend fun incrementInvitesAccepted() {
        updateUserStats { it.copy(invitesAccepted = it.invitesAccepted + 1) }
        checkBadges()
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
