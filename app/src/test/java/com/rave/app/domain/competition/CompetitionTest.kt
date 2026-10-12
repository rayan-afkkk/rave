package com.rave.app.domain.competition

import com.rave.app.domain.model.ActivitySummary
import com.rave.app.domain.model.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CompetitionTest {

    private fun s(uid: String, value: Double, updated: Long = 0, team: Int? = null, reached: Long? = null) =
        ParticipantScore(uid, uid, value, team, updated, reached)

    @Test
    fun `ranks by value with shared ranks for ties`() {
        val ranked = CompetitionRanking.rank(listOf(s("a", 5.0), s("b", 9.0), s("c", 5.0, updated = -1), s("d", 1.0)), CompetitionType.DISTANCE)
        assertEquals(listOf("b", "c", "a", "d"), ranked.map { it.score.uid })
        assertEquals(listOf(1, 2, 2, 4), ranked.map { it.rank })
    }

    @Test
    fun `position change arrows`() {
        val ranked = CompetitionRanking.rank(listOf(s("a", 10.0), s("b", 8.0), s("c", 3.0)), CompetitionType.DISTANCE,
            previousRanks = mapOf("a" to 3, "b" to 1, "c" to 2))
        assertEquals(mapOf("a" to 2, "b" to -1, "c" to -1), ranked.associate { it.score.uid to it.change })
    }

    @Test
    fun `gap to the next person ahead`() {
        val ranked = CompetitionRanking.rank(listOf(s("me", 3.6), s("ali", 5.0), s("zara", 9.0)), CompetitionType.DISTANCE)
        val (ahead, gap) = CompetitionRanking.gapToNext(ranked, "me")!!
        assertEquals("ali", ahead.score.uid)
        assertEquals(1.4, gap, 1e-9)
        assertNull(CompetitionRanking.gapToNext(ranked, "zara"))
    }

    @Test
    fun `target race orders finishers by finish time`() {
        val ranked = CompetitionRanking.rank(
            listOf(s("slow", 50.0, reached = 200), s("fast", 51.0, reached = 100), s("far", 40.0), s("near", 45.0)),
            CompetitionType.TARGET_RACE,
        )
        assertEquals(listOf("fast", "slow", "near", "far"), ranked.map { it.score.uid })
        assertEquals(listOf(1, 2, 3, 4), ranked.map { it.rank })
    }

    @Test
    fun `team totals and overtakers`() {
        val totals = CompetitionRanking.teamTotals(listOf(s("a", 2.0, team = 0), s("b", 3.0, team = 0), s("c", 4.0, team = 1)))
        assertEquals(mapOf(0 to 5.0, 1 to 4.0), totals)
        val after = CompetitionRanking.rank(listOf(s("me", 5.0), s("ali", 6.0)), CompetitionType.DISTANCE)
        assertEquals(listOf("ali"), CompetitionRanking.overtakers(mapOf("me" to 1, "ali" to 2), after, "me"))
    }

    private val comp = Competition(
        id = "c", name = "Test", type = CompetitionType.DISTANCE, mode = CompetitionMode.GROUP,
        startAtMs = 1_000, endAtMs = 1_000_000_000, createdBy = "me",
    )

    @Test
    fun `anti cheat rejects unrealistic runs and flags auto detected walks`() {
        val tooFast = ActivitySummary(1, ActivityType.RUN, 5_000, 10_000.0, 1200, false) // 30 km/h
        val auto = ActivitySummary(2, ActivityType.WALK, 5_000, 2_000.0, 1500, true)
        val ok = ActivitySummary(3, ActivityType.RUN, 5_000, 5_000.0, 1500, false)
        assertEquals(Eligibility.TOO_FAST, AntiCheat.check(tooFast))
        assertEquals(Eligibility.FLAGGED_AUTO_DETECTED, AntiCheat.check(auto))
        val day = LocalDate.of(1970, 1, 1)
        val result = CompetitionScoring.score(comp, listOf(tooFast, auto, ok), emptyList(), day, day.plusDays(30))
        assertEquals(7.0, result.value, 1e-9)
        assertEquals(1, result.flagged)
    }

    @Test
    fun `activities outside the window do not count`() {
        val before = ActivitySummary(1, ActivityType.WALK, 500, 3_000.0, 1800, false)
        val day = LocalDate.of(1970, 1, 1)
        assertEquals(0.0, CompetitionScoring.score(comp, listOf(before), emptyList(), day, day).value, 0.0)
    }

    @Test
    fun `habit competitions count matching habits and streaks`() {
        val start = LocalDate.of(2026, 1, 1)
        val days = listOf(
            HabitDay(start, listOf("Read", "Water")),
            HabitDay(start.plusDays(1), listOf("read")),
            HabitDay(start.plusDays(3), listOf("Water")),
        )
        val habits = comp.copy(type = CompetitionType.HABITS_COMPLETED, habitNames = listOf("Read"))
        assertEquals(2.0, CompetitionScoring.score(habits, emptyList(), days, start, start.plusDays(6)).value, 0.0)
        val streak = comp.copy(type = CompetitionType.LONGEST_STREAK)
        assertEquals(2.0, CompetitionScoring.score(streak, emptyList(), days, start, start.plusDays(6)).value, 0.0)
    }
}
