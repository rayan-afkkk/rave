package com.rave.app.domain.competition

/** Pure ranking helpers for competition leaderboards. */
object CompetitionRanking {

    /**
     * Ranks by value (desc). Equal values share a rank ("1, 1, 3"); within a tie whoever got
     * there first (earlier lastUpdated) is listed first. For TARGET_RACE, finishers come first
     * ordered by finish time, each with a distinct rank.
     *
     * [previousRanks] (uid → rank) produces the ↑/↓ change; newcomers get 0.
     */
    fun rank(
        scores: List<ParticipantScore>,
        type: CompetitionType,
        previousRanks: Map<String, Int> = emptyMap(),
    ): List<RankedParticipant> {
        if (type == CompetitionType.TARGET_RACE) return rankRace(scores, previousRanks)

        val sorted = scores.sortedWith(compareByDescending<ParticipantScore> { it.value }.thenBy { it.lastUpdatedMs })
        val result = mutableListOf<RankedParticipant>()
        sorted.forEachIndexed { index, score ->
            val rank = if (index > 0 && sorted[index - 1].value == score.value) result[index - 1].rank else index + 1
            result += RankedParticipant(score, rank, change(previousRanks[score.uid], rank))
        }
        return result
    }

    private fun rankRace(scores: List<ParticipantScore>, previousRanks: Map<String, Int>): List<RankedParticipant> {
        val finishers = scores.filter { it.reachedTargetAtMs != null }.sortedBy { it.reachedTargetAtMs }
        val others = scores.filter { it.reachedTargetAtMs == null }
        val finisherRanks = finishers.mapIndexed { i, s -> RankedParticipant(s, i + 1, change(previousRanks[s.uid], i + 1)) }
        val othersRanked = rank(others, CompetitionType.DISTANCE).map { r ->
            val rank = r.rank + finishers.size
            r.copy(rank = rank, change = change(previousRanks[r.score.uid], rank))
        }
        return finisherRanks + othersRanked
    }

    private fun change(previous: Int?, now: Int): Int = if (previous == null) 0 else previous - now

    /** The participant directly ahead of [uid] with a strictly higher value, and the gap to them. */
    fun gapToNext(ranked: List<RankedParticipant>, uid: String): Pair<RankedParticipant, Double>? {
        val me = ranked.firstOrNull { it.score.uid == uid } ?: return null
        val ahead = ranked.filter { it.score.value > me.score.value }.minByOrNull { it.score.value } ?: return null
        return ahead to (ahead.score.value - me.score.value)
    }

    /** Combined value per team index for TEAMS mode. */
    fun teamTotals(scores: List<ParticipantScore>): Map<Int, Double> =
        scores.filter { it.team != null }.groupBy { it.team!! }.mapValues { (_, s) -> s.sumOf { it.value } }

    /** Users who were ahead of [uid] now but behind before: they just overtook. */
    fun overtakers(before: Map<String, Int>, after: List<RankedParticipant>, uid: String): List<String> {
        val myBefore = before[uid] ?: return emptyList()
        val myNow = after.firstOrNull { it.score.uid == uid }?.rank ?: return emptyList()
        return after.filter { r ->
            val prev = before[r.score.uid]
            r.score.uid != uid && prev != null && prev > myBefore && r.rank < myNow
        }.map { it.score.uid }
    }
}
