package com.rave.app.domain.competition

import com.rave.app.domain.model.ActivityType

enum class CompetitionType {
    /** Most kilometres (walk / run / both). */
    DISTANCE,
    /** Most days with at least one activity or completed habit. */
    ACTIVE_DAYS,
    /** Most habit completions (optionally only certain habits). */
    HABITS_COMPLETED,
    /** Longest daily habit streak inside the period. */
    LONGEST_STREAK,
    /** First to reach a target distance. */
    TARGET_RACE,
}

enum class CompetitionMode { DUEL, GROUP, TEAMS }

enum class CompetitionStatus { UPCOMING, ACTIVE, ENDED }

/** Which activities count for distance-based competitions. Null = both. */
typealias ActivityFilter = ActivityType?

data class Competition(
    val id: String,
    val name: String,
    val type: CompetitionType,
    val mode: CompetitionMode,
    val activityFilter: ActivityType? = null,
    /** TARGET_RACE goal in km. */
    val targetKm: Double = 0.0,
    /** HABITS_COMPLETED: only habits with these names count. Empty = all habits. */
    val habitNames: List<String> = emptyList(),
    val startAtMs: Long,
    val endAtMs: Long,
    val stake: String? = null,
    val createdBy: String,
    val members: List<String> = emptyList(),
    val invited: List<String> = emptyList(),
    /** Custom team names; empty = default labels from resources. */
    val teamNames: List<String> = emptyList(),
) {
    fun statusAt(nowMs: Long): CompetitionStatus = when {
        nowMs < startAtMs -> CompetitionStatus.UPCOMING
        nowMs >= endAtMs -> CompetitionStatus.ENDED
        else -> CompetitionStatus.ACTIVE
    }

    companion object {
        const val MAX_PARTICIPANTS = 20
    }
}

/** A participant's current score as stored in Firestore. */
data class ParticipantScore(
    val uid: String,
    val name: String,
    val value: Double,
    val team: Int? = null,
    val lastUpdatedMs: Long = 0,
    /** TARGET_RACE: when the target was reached. */
    val reachedTargetAtMs: Long? = null,
    val flaggedActivities: Int = 0,
)

data class RankedParticipant(
    val score: ParticipantScore,
    val rank: Int,
    /** Positive = moved up (e.g. +2 shows "↑2"), negative = moved down. */
    val change: Int,
)
