package com.rave.app.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rave.app.domain.model.LatLngPoint
import com.rave.app.ui.components.BentoTileShape
import com.rave.app.ui.components.RouteCanvas
import com.rave.app.ui.components.TileMapView
import com.rave.app.ui.theme.RaveTheme
import kotlin.math.ln
import kotlin.math.max

/** Bento sand map card fitted to the route bounds (MapTiler & OpenStreetMap tiles), route in orange. */
@Composable
fun RouteMapCard(segments: List<List<LatLngPoint>>, modifier: Modifier = Modifier, height: Dp = 170.dp) {
    val all = remember(segments) { segments.flatten() }
    val routeColor = RaveTheme.colors.tileOrange.container

    Box(modifier = modifier.fillMaxWidth().height(height).clip(BentoTileShape).background(RaveTheme.colors.tileSand.container)) {
        if (all.size < 2) {
            RouteCanvas(
                segments = segments,
                color = routeColor,
                modifier = Modifier.fillMaxSize().padding(24.dp),
                strokeWidth = 7.dp,
                showEndpoints = true,
                endpointColor = RaveTheme.colors.tileInk.container
            )
            return@Box
        }

        val minLat = all.minOf { it.lat }
        val maxLat = all.maxOf { it.lat }
        val minLng = all.minOf { it.lng }
        val maxLng = all.maxOf { it.lng }

        val centerLat = (minLat + maxLat) / 2.0
        val centerLng = (minLng + maxLng) / 2.0

        val latDiff = (maxLat - minLat).coerceAtLeast(0.001)
        val lngDiff = (maxLng - minLng).coerceAtLeast(0.001)
        val maxDiff = max(latDiff, lngDiff)
        // Zoom formula for fitting bounding box
        val zoom = (ln(360.0 / maxDiff) / ln(2.0) - 1.2).toFloat().coerceIn(3f, 18f)

        TileMapView(
            centerLat = centerLat,
            centerLng = centerLng,
            initialZoom = zoom,
            segments = segments,
            startLocation = all.first(),
            currentLocation = all.last(),
            interactive = false,
            routeColor = routeColor,
            modifier = Modifier.fillMaxSize()
        )
    }
}
