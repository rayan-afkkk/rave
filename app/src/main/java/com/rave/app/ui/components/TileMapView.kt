package com.rave.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.rave.app.BuildConfig
import com.rave.app.R
import okhttp3.OkHttpClient
import com.rave.app.domain.model.LatLngPoint
import com.rave.app.ui.theme.RaveTheme
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.tan

// Standard Web Mercator tile size on Android screen (in dp)
private val TILE_SIZE_DP = 256.dp

/**
 * Pixel-perfect Slippy Map that streams MapTiler & OpenStreetMap tiles
 * with seamless tile alignment, sub-pixel accuracy, and real-time GPS tracking.
 */
@Composable
fun TileMapView(
    centerLat: Double,
    centerLng: Double,
    modifier: Modifier = Modifier,
    initialZoom: Float = 16f,
    segments: List<List<LatLngPoint>> = emptyList(),
    currentLocation: LatLngPoint? = null,
    startLocation: LatLngPoint? = null,
    guidePath: List<LatLngPoint> = emptyList(),
    circles: List<Pair<LatLngPoint, Float>> = emptyList(),
    interactive: Boolean = true,
    followLocation: Boolean = false,
    routeColor: Color = MaterialTheme.colorScheme.primary,
    onUserPanned: (() -> Unit)? = null,
    onMapClick: ((lat: Double, lng: Double) -> Unit)? = null,
) {
    var zoom by remember { mutableFloatStateOf(initialZoom.coerceIn(3f, 19f)) }
    var currentCenterLat by remember { mutableStateOf(centerLat) }
    var currentCenterLng by remember { mutableStateOf(centerLng) }

    LaunchedEffect(centerLat, centerLng) {
        if (followLocation || !interactive) {
            currentCenterLat = centerLat
            currentCenterLng = centerLng
        }
    }

    LaunchedEffect(initialZoom) {
        if (!interactive) {
            zoom = initialZoom.coerceIn(3f, 19f)
        }
    }

    val dark = RaveTheme.isDark
    val density = LocalDensity.current
    val context = LocalContext.current
    val tileLoader = remember { TileImageLoader.get(context) }
    val provider = remember { TileProvider.resolve() }
    var anyTileLoaded by remember { mutableStateOf(false) }
    var anyTileFailed by remember { mutableStateOf(false) }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by pulse.animateFloat(
        initialValue = 6f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "pulseRadius",
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "pulseAlpha",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(if (dark) Color(0xFF1E1E1E) else Color(0xFFF2EFE9))
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val tileSizePx = with(density) { TILE_SIZE_DP.toPx() }

        val gestureModifier = if (interactive) {
            Modifier
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomFactor, _ ->
                        zoom = (zoom * zoomFactor).coerceIn(3f, 19f)
                        val intZoom = zoom.roundToInt().coerceIn(3, 19)
                        val scale = 2.0.pow(intZoom.toDouble()) * tileSizePx

                        val dLng = -(pan.x.toDouble() / scale) * 360.0
                        currentCenterLng = (currentCenterLng + dLng).coerceIn(-180.0, 180.0)

                        val currentWorldY = latToWorldY(currentCenterLat, scale)
                        val newWorldY = currentWorldY - pan.y.toDouble()
                        currentCenterLat = worldYToLat(newWorldY, scale)

                        if (pan.getDistance() > 1f) {
                            onUserPanned?.invoke()
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        onMapClick?.let { onClick ->
                            val intZoom = zoom.roundToInt().coerceIn(3, 19)
                            val scale = 2.0.pow(intZoom.toDouble()) * tileSizePx
                            val centerWorldX = lngToWorldX(currentCenterLng, scale)
                            val centerWorldY = latToWorldY(currentCenterLat, scale)

                            val tapWorldX = centerWorldX - (widthPx / 2.0) + tapOffset.x
                            val tapWorldY = centerWorldY - (heightPx / 2.0) + tapOffset.y

                            val tappedLng = (tapWorldX / scale * 360.0) - 180.0
                            val tappedLat = worldYToLat(tapWorldY, scale)
                            onClick(tappedLat, tappedLng)
                        }
                    }
                }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(gestureModifier)
        ) {
            val intZoom = zoom.roundToInt().coerceIn(3, 19)
            val scale = 2.0.pow(intZoom.toDouble()) * tileSizePx
            val centerWorldX = lngToWorldX(currentCenterLng, scale)
            val centerWorldY = latToWorldY(currentCenterLat, scale)
            val numTiles = 1 shl intZoom

            val minTileX = floor((centerWorldX - widthPx / 2.0) / tileSizePx).toInt()
            val maxTileX = floor((centerWorldX + widthPx / 2.0) / tileSizePx).toInt()
            val minTileY = floor((centerWorldY - heightPx / 2.0) / tileSizePx).toInt().coerceIn(0, numTiles - 1)
            val maxTileY = floor((centerWorldY + heightPx / 2.0) / tileSizePx).toInt().coerceIn(0, numTiles - 1)

            for (tileY in minTileY..maxTileY) {
                for (rawTileX in minTileX..maxTileX) {
                    val tileX = ((rawTileX % numTiles) + numTiles) % numTiles
                    val screenTileLeftPx = (rawTileX * tileSizePx - centerWorldX + widthPx / 2.0).roundToInt()
                    val screenTileTopPx = (tileY * tileSizePx - centerWorldY + heightPx / 2.0).roundToInt()

                    val tileUrl = provider.url(intZoom, tileX, tileY, dark)

                    AsyncImage(
                        model = tileUrl,
                        imageLoader = tileLoader,
                        onSuccess = { if (!anyTileLoaded) anyTileLoaded = true },
                        onError = { if (!anyTileFailed) anyTileFailed = true },
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .requiredSize(TILE_SIZE_DP)
                            .offset { IntOffset(screenTileLeftPx, screenTileTopPx) }
                    )
                }
            }

            // Path & Markers Overlay Canvas
            val surface = MaterialTheme.colorScheme.surface
            val tertiary = MaterialTheme.colorScheme.tertiary

            Canvas(modifier = Modifier.fillMaxSize()) {
                fun toScreen(pt: LatLngPoint): Offset {
                    val ptX = lngToWorldX(pt.lng, scale)
                    val ptY = latToWorldY(pt.lat, scale)
                    return Offset(
                        x = (ptX - centerWorldX + widthPx / 2.0).toFloat(),
                        y = (ptY - centerWorldY + heightPx / 2.0).toFloat(),
                    )
                }

                // Circles (Privacy Zones)
                circles.forEach { (center, radiusM) ->
                    val screenCenter = toScreen(center)
                    val metersPerPx = (156543.03392 * cos(center.lat * PI / 180.0) / (1 shl intZoom)).toFloat() / density.density
                    val radiusPx = (radiusM / metersPerPx).coerceAtLeast(12f)
                    drawCircle(
                        color = routeColor.copy(alpha = 0.25f),
                        radius = radiusPx,
                        center = screenCenter,
                    )
                    drawCircle(
                        color = routeColor,
                        radius = radiusPx,
                        center = screenCenter,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }

                // Guide Path
                if (guidePath.size >= 2) {
                    val path = Path()
                    val first = toScreen(guidePath.first())
                    path.moveTo(first.x, first.y)
                    for (i in 1 until guidePath.size) {
                        val pt = toScreen(guidePath[i])
                        path.lineTo(pt.x, pt.y)
                    }
                    drawPath(
                        path = path,
                        color = Color.Gray.copy(alpha = 0.45f),
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }

                // Recorded Route Segments
                segments.filter { it.size >= 2 }.forEach { segment ->
                    val path = Path()
                    val first = toScreen(segment.first())
                    path.moveTo(first.x, first.y)
                    for (i in 1 until segment.size) {
                        val pt = toScreen(segment[i])
                        path.lineTo(pt.x, pt.y)
                    }
                    drawPath(
                        path = path,
                        color = routeColor,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }

                // Start Marker (Green / Tertiary dot)
                startLocation?.let { start ->
                    val screenPt = toScreen(start)
                    drawCircle(color = surface, radius = 9.dp.toPx(), center = screenPt)
                    drawCircle(color = tertiary, radius = 6.dp.toPx(), center = screenPt)
                }

                // Live Current Location Radar & Marker
                currentLocation?.let { current ->
                    val screenPt = toScreen(current)
                    // Outer pulsing radar
                    drawCircle(
                        color = routeColor.copy(alpha = pulseAlpha),
                        radius = (pulseRadius * density.density),
                        center = screenPt,
                    )
                    // White border
                    drawCircle(color = surface, radius = 8.dp.toPx(), center = screenPt)
                    // Center accent dot
                    drawCircle(color = routeColor, radius = 5.5f.dp.toPx(), center = screenPt)
                }
            }

            // No tile has loaded yet and some failed: tell the user instead of showing a blank map.
            if (!anyTileLoaded && anyTileFailed) {
                Text(
                    text = stringResource(R.string.map_tiles_failed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), shape = MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            // Attribution required by the tile provider
            Text(
                text = provider.attribution,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), shape = MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/** Tile source in priority order: Mapbox -> MapTiler -> TomTom -> OpenStreetMap (no key needed). */
private enum class TileProvider(val attribution: String) {
    MAPBOX("© Mapbox © OpenStreetMap"),
    MAPTILER("© MapTiler © OpenStreetMap contributors"),
    TOMTOM("© TomTom"),
    OSM("© OpenStreetMap contributors");

    /** Uses @2x retina tiles where the provider offers them. */
    fun url(zoom: Int, x: Int, y: Int, dark: Boolean): String = when (this) {
        MAPBOX -> {
            val styleId = if (dark) "dark-v11" else "streets-v12"
            "https://api.mapbox.com/styles/v1/mapbox/$styleId/tiles/256/$zoom/$x/$y@2x?access_token=${BuildConfig.MAPBOX_ACCESS_TOKEN}"
        }
        MAPTILER -> {
            val style = if (dark) "dataviz-dark" else "streets-v2"
            "https://api.maptiler.com/maps/$style/$zoom/$x/$y@2x.png?key=${BuildConfig.MAPTILER_API_KEY}"
        }
        TOMTOM -> "https://api.tomtom.com/map/1/tile/basic/main/$zoom/$x/$y.png?key=${BuildConfig.TOMTOM_API_KEY}"
        OSM -> "https://tile.openstreetmap.org/$zoom/$x/$y.png"
    }

    companion object {
        private fun configured(key: String) = key.isNotBlank() && !key.startsWith("YOUR_")

        fun resolve(): TileProvider = when {
            configured(BuildConfig.MAPBOX_ACCESS_TOKEN) -> MAPBOX
            configured(BuildConfig.MAPTILER_API_KEY) -> MAPTILER
            configured(BuildConfig.TOMTOM_API_KEY) -> TOMTOM
            else -> OSM
        }
    }
}

/**
 * Image loader just for map tiles:
 * - sends an identifying User-Agent (OpenStreetMap blocks the generic OkHttp one, which left the map blank),
 * - keeps up to 250 MB of tiles on disk and reuses them without re-checking, so areas you have
 *   already seen still show when you are offline.
 */
private object TileImageLoader {
    @Volatile private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader = instance ?: synchronized(this) {
        instance ?: build(context.applicationContext).also { instance = it }
    }

    private fun build(context: Context): ImageLoader {
        val userAgent = "Rave/${BuildConfig.VERSION_NAME} (Android; ${BuildConfig.APPLICATION_ID})"
        return ImageLoader.Builder(context)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build())
                    }
                    .build()
            }
            .memoryCache { MemoryCache.Builder(context).maxSizePercent(0.15).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("map_tiles"))
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
}

private fun lngToWorldX(lng: Double, scale: Double): Double {
    return (lng + 180.0) / 360.0 * scale
}

private fun latToWorldY(lat: Double, scale: Double): Double {
    val latRad = lat.coerceIn(-85.0511, 85.0511) * PI / 180.0
    return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * scale
}

private fun worldYToLat(worldY: Double, scale: Double): Double {
    val n = PI - 2.0 * PI * (worldY / scale)
    return 180.0 / PI * atan(sinh(n))
}
