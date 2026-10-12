package com.rave.app.domain.tracking

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoMath {
    private const val EARTH_RADIUS_M = 6_371_008.8

    /** Great-circle distance in metres between two WGS84 coordinates. */
    fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /** Equirectangular projection to local metres around [refLat]; good enough for short tracks. */
    fun toLocalMeters(lat: Double, lng: Double, refLat: Double, refLng: Double): Pair<Double, Double> {
        val x = Math.toRadians(lng - refLng) * EARTH_RADIUS_M * cos(Math.toRadians(refLat))
        val y = Math.toRadians(lat - refLat) * EARTH_RADIUS_M
        return x to y
    }
}
