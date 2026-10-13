package com.rave.app.domain.tracking

import com.rave.app.domain.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RouteCleanerTest {

    private fun pt(lat: Double, lng: Double, segment: Int = 0, accuracy: Float = 5f, t: Long = 0) =
        TrackPoint(lat = lat, lng = lng, accuracy = accuracy, timeMs = t, segment = segment)

    @Test
    fun `pauses split the route instead of joining with a straight line`() {
        val a = (0..10).map { pt(51.0 + it * 0.0001, 0.0, segment = 0) }
        val b = (0..10).map { pt(51.01 + it * 0.0001, 0.01, segment = 1) }
        assertEquals(2, RouteCleaner.clean(a + b).size)
    }

    @Test
    fun `inaccurate fixes are dropped`() {
        val good = (0..10).map { pt(51.0 + it * 0.0001, 0.0) }
        val bad = pt(51.0005, 0.01, accuracy = 80f) // ~700 m off to the side
        val cleaned = RouteCleaner.clean(good.take(5) + bad + good.drop(5)).flatten()
        assertTrue(cleaned.all { abs(it.lng) < 0.001 })
    }

    @Test
    fun `jitter is smoothed towards the true line`() {
        // Straight line north with ±6 m east-west zig-zag noise.
        val noisy = (0..60).map { i -> pt(51.0 + i * 0.00005, if (i % 2 == 0) 0.00009 else -0.00009) }
        val cleaned = RouteCleaner.clean(noisy).flatten()
        val inner = cleaned.drop(1).dropLast(1)
        val maxNoise = inner.maxOf { abs(it.lng) }
        assertTrue("noise $maxNoise should shrink well below 0.00009", maxNoise < 0.00004)
    }

    @Test
    fun `smoothing keeps the exact start and end`() {
        val pts = (0..9).map { com.rave.app.domain.model.LatLngPoint(51.0 + it * 0.0001, if (it % 2 == 0) 0.0001 else 0.0) }
        val s = RouteCleaner.smooth(pts)
        assertEquals(pts.first(), s.first())
        assertEquals(pts.last(), s.last())
    }
}
