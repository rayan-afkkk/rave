package com.rave.app.domain.tracking

import com.rave.app.domain.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackFilterTest {
    private val filter = TrackFilter()
    private val origin = TrackPoint(51.0, 0.0, accuracy = 5f, timeMs = 0)

    @Test
    fun `rejects poor accuracy`() {
        assertEquals(FilterResult.PoorAccuracy, filter.evaluate(origin, origin.copy(accuracy = 30f, timeMs = 1000)))
    }

    @Test
    fun `rejects impossible jumps`() {
        // 111 m in 2 s = 55 m/s
        val jump = origin.copy(lat = 51.001, timeMs = 2000)
        assertEquals(FilterResult.ImpossibleJump, filter.evaluate(origin, jump))
    }

    @Test
    fun `ignores jitter while standing`() {
        val jitter = origin.copy(lat = 51.00001, timeMs = 2000) // ~1 m
        assertEquals(FilterResult.Stationary, filter.evaluate(origin, jitter))
    }

    @Test
    fun `accepts realistic movement`() {
        val next = origin.copy(lat = 51.0001, timeMs = 5000) // ~11 m in 5 s
        val result = filter.evaluate(origin, next)
        assertTrue(result is FilterResult.Accept)
        assertEquals(11.1, (result as FilterResult.Accept).distanceM, 0.2)
    }

    @Test
    fun `first point after pause adds no distance`() {
        val resumed = origin.copy(lat = 51.01, timeMs = 600_000, segment = 1)
        assertEquals(FilterResult.Accept(0.0), filter.evaluate(origin, resumed))
    }
}
