package com.rave.app.domain.tracking

import com.rave.app.domain.model.LatLngPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolylinePrivacyTest {

    @Test
    fun `encodes the Google reference example`() {
        val pts = listOf(LatLngPoint(38.5, -120.2), LatLngPoint(40.7, -120.95), LatLngPoint(43.252, -126.453))
        assertEquals("_p~iF~ps|U_ulLnnqC_mqNvxq`@", Polyline.encode(pts))
        val decoded = Polyline.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3, decoded.size)
        assertEquals(43.252, decoded[2].lat, 1e-5)
        assertEquals(-126.453, decoded[2].lng, 1e-5)
    }

    @Test
    fun `simplify drops collinear points`() {
        val line = (0..50).map { LatLngPoint(51.0 + it * 0.0001, 0.0) }
        assertEquals(2, Polyline.simplify(line).size)
    }

    private val route = (0..30).map { LatLngPoint(51.0 + it * 0.0005, 0.0) } // ~55.6 m apart, ~1.67 km

    @Test
    fun `trims first and last 200 m`() {
        val segments = PrivacyFilter.apply(route, zones = emptyList())
        assertEquals(1, segments.size)
        val first = segments[0].first()
        val start = route.first()
        assertTrue(GeoMath.haversine(start.lat, start.lng, first.lat, first.lng) >= 200)
        val last = segments[0].last()
        val end = route.last()
        assertTrue(GeoMath.haversine(end.lat, end.lng, last.lat, last.lng) >= 200)
    }

    @Test
    fun `short routes are hidden completely`() {
        assertTrue(PrivacyFilter.apply(route.take(5), zones = emptyList()).isEmpty())
    }

    @Test
    fun `zones split the route into separate segments`() {
        val mid = route[15]
        val segments = PrivacyFilter.apply(route, listOf(PrivacyArea(mid.lat, mid.lng, 120.0)), trimEnds = false)
        assertEquals(2, segments.size)
        segments.flatten().forEach { assertTrue(GeoMath.haversine(it.lat, it.lng, mid.lat, mid.lng) > 120) }
    }
}
