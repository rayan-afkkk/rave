package com.rave.app.domain.tracking

import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.model.TrackPoint
import com.rave.app.domain.util.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackStatsTest {

    /** ~111.2 m per 0.001° of latitude. Points heading north at a steady pace. */
    private fun straightLine(count: Int, secondsPerPoint: Long, segment: (Int) -> Int = { 0 }) =
        (0 until count).map { i ->
            TrackPoint(lat = 51.0 + i * 0.001, lng = 0.0, timeMs = i * secondsPerPoint * 1000, segment = segment(i))
        }

    @Test
    fun `haversine matches known distance`() {
        // London -> Paris ≈ 343.5 km
        val d = GeoMath.haversine(51.5074, -0.1278, 48.8566, 2.3522)
        assertEquals(343_500.0, d, 1_000.0)
    }

    @Test
    fun `distance sums consecutive points`() {
        val pts = straightLine(11, 30)
        assertEquals(1_112.0, TrackStats.distance(pts), 2.0)
    }

    @Test
    fun `distance is not counted across a pause`() {
        val pts = straightLine(11, 30) { i -> if (i < 5) 0 else 1 }
        // 4 gaps in segment 0, 5 gaps in segment 1 = 9 × 111.2 m
        assertEquals(1_000.8, TrackStats.distance(pts), 2.0)
    }

    @Test
    fun `pace is moving time over distance`() {
        val pace = TrackStats.paceSecondsPerKm(movingTimeS = 1_800.0, distanceM = 5_000.0)!!
        assertEquals(360.0, pace, 0.001)
        assertEquals("6:00", Format.pace(pace, DistanceUnit.KM))
        assertEquals("9:39", Format.pace(pace, DistanceUnit.MILES))
        assertNull(TrackStats.paceSecondsPerKm(100.0, 0.0))
    }

    @Test
    fun `moving time ignores stationary intervals`() {
        val moving = straightLine(5, 30)
        val last = moving.last()
        val standing = (1..4).map { last.copy(timeMs = last.timeMs + it * 30_000L) }
        assertEquals(120.0, TrackStats.movingTimeS(moving + standing), 0.01)
    }

    @Test
    fun `splits per km with partial tail`() {
        val pts = straightLine(30, 40) // 29 × 111.2 m ≈ 3.22 km, 40 s per 111 m ≈ 6:00/km
        val splits = TrackStats.splits(pts)
        assertEquals(4, splits.size)
        assertEquals(1000.0, splits[0].distanceM, 0.01)
        assertEquals(360.0, splits[0].paceSecondsPer(1000.0), 2.0)
        assertTrue(splits.last().distanceM < 1000.0)
    }

    @Test
    fun `fastest window finds the quick kilometre`() {
        // First 1.1 km slow (60 s per point), then 1.1 km fast (20 s per point).
        var t = 0L
        val pts = (0 until 21).map { i ->
            if (i > 0) t += if (i <= 10) 60_000 else 20_000
            TrackPoint(lat = 51.0 + i * 0.001, lng = 0.0, timeMs = t)
        }
        val fastest = TrackStats.fastestWindowS(pts, 1000.0)
        assertNotNull(fastest)
        assertEquals(1000.0 / 111.2 * 20, fastest!!, 3.0)
        assertNull(TrackStats.fastestWindowS(pts, 5000.0))
    }

    @Test
    fun `elevation gain ignores small noise`() {
        val alts = listOf(10.0, 11.0, 10.0, 11.5, 15.0, 14.0, 20.0)
        val pts = alts.mapIndexed { i, a -> TrackPoint(51.0, 0.0, altitude = a, timeMs = i * 1000L) }
        assertEquals(10.0, TrackStats.elevationGain(pts), 0.01)
    }

    @Test
    fun `calories use MET times weight times hours`() {
        // Walk at 10 min/km for 1 hour, 70 kg -> MET 4.3 -> 301 kcal
        val kcal = Calories.estimate(com.rave.app.domain.model.ActivityType.WALK, 70.0, 3600.0, 6000.0)
        assertEquals(301, kcal)
        assertEquals(0, Calories.estimate(com.rave.app.domain.model.ActivityType.RUN, 70.0, 0.0, 0.0))
    }
}
