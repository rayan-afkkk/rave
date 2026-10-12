package com.rave.app.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.rave.app.BuildConfig
import com.rave.app.R
import com.rave.app.domain.model.LatLngPoint
import com.rave.app.ui.components.RouteCanvas
import com.rave.app.ui.theme.RaveTheme

/** Static (lite-mode) map fitted to the route bounds; falls back to a drawn route without a Maps key. */
@Composable
fun RouteMapCard(segments: List<List<LatLngPoint>>, modifier: Modifier = Modifier, height: Dp = 220.dp) {
    val shape = MaterialTheme.shapes.large
    val all = segments.flatten()
    val routeColor = MaterialTheme.colorScheme.primary
    Box(modifier.fillMaxWidth().height(height).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (BuildConfig.MAPS_API_KEY.isBlank() || all.size < 2) {
            RouteCanvas(segments, routeColor, Modifier.fillMaxSize().padding(24.dp), strokeWidth = 5.dp, showEndpoints = true,
                endpointColor = MaterialTheme.colorScheme.tertiary)
            return@Box
        }
        val context = LocalContext.current
        val dark = RaveTheme.isDark
        val camera = rememberCameraPositionState()
        val bounds = remember(all) {
            LatLngBounds.builder().apply { all.forEach { include(LatLng(it.lat, it.lng)) } }.build()
        }
        val properties = remember(dark) {
            MapProperties(mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, if (dark) R.raw.map_style_dark else R.raw.map_style_light))
        }
        val surface = MaterialTheme.colorScheme.surface
        val endColor = MaterialTheme.colorScheme.tertiary
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            properties = properties,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false, scrollGesturesEnabled = false, zoomGesturesEnabled = false,
                rotationGesturesEnabled = false, tiltGesturesEnabled = false, mapToolbarEnabled = false,
            ),
            googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
            onMapLoaded = { camera.move(CameraUpdateFactory.newLatLngBounds(bounds, 64)) },
        ) {
            segments.filter { it.size >= 2 }.forEach { seg ->
                Polyline(points = seg.map { LatLng(it.lat, it.lng) }, color = routeColor, width = 12f,
                    jointType = JointType.ROUND, startCap = RoundCap(), endCap = RoundCap())
            }
            all.first().let { Circle(center = LatLng(it.lat, it.lng), radius = 6.0, fillColor = endColor, strokeColor = surface, strokeWidth = 4f) }
            all.last().let { Circle(center = LatLng(it.lat, it.lng), radius = 6.0, fillColor = routeColor, strokeColor = surface, strokeWidth = 4f) }
        }
    }
}
