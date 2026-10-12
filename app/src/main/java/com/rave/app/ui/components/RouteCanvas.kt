package com.rave.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rave.app.domain.model.LatLngPoint
import com.rave.app.domain.tracking.GeoMath

/**
 * Draws a route shape without map tiles: used for list thumbnails, feed items and share
 * cards (works offline and without a Maps key). Segments are drawn separately so privacy
 * gaps stay gaps.
 */
@Composable
fun RouteCanvas(
    segments: List<List<LatLngPoint>>,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 4.dp,
    padding: Dp = 8.dp,
    showEndpoints: Boolean = false,
    endpointColor: Color = color,
) {
    val projected = remember(segments) { project(segments) }
    Canvas(modifier) {
        drawRoute(projected, color, strokeWidth.toPx(), padding.toPx(), showEndpoints, endpointColor)
    }
}

/** Route projected to local metres with a y-down axis, ready to be scaled into any box. */
class ProjectedRoute(val segments: List<List<Offset>>, val width: Float, val height: Float, val minX: Float, val minY: Float)

fun project(segments: List<List<LatLngPoint>>): ProjectedRoute? {
    val all = segments.flatten()
    if (all.size < 2) return null
    val ref = all.first()
    val xy = segments.map { seg ->
        seg.map { p ->
            val (x, y) = GeoMath.toLocalMeters(p.lat, p.lng, ref.lat, ref.lng)
            Offset(x.toFloat(), -y.toFloat())
        }
    }
    val flat = xy.flatten()
    val minX = flat.minOf { it.x }
    val maxX = flat.maxOf { it.x }
    val minY = flat.minOf { it.y }
    val maxY = flat.maxOf { it.y }
    return ProjectedRoute(xy, (maxX - minX).coerceAtLeast(1f), (maxY - minY).coerceAtLeast(1f), minX, minY)
}

fun DrawScope.drawRoute(
    route: ProjectedRoute?,
    color: Color,
    strokePx: Float,
    paddingPx: Float,
    showEndpoints: Boolean = false,
    endpointColor: Color = color,
) {
    route ?: return
    val availW = size.width - paddingPx * 2
    val availH = size.height - paddingPx * 2
    val scale = minOf(availW / route.width, availH / route.height)
    val offsetX = paddingPx + (availW - route.width * scale) / 2
    val offsetY = paddingPx + (availH - route.height * scale) / 2
    fun map(o: Offset) = Offset(offsetX + (o.x - route.minX) * scale, offsetY + (o.y - route.minY) * scale)

    route.segments.filter { it.size >= 2 }.forEach { seg ->
        val path = Path()
        seg.forEachIndexed { i, o ->
            val p = map(o)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, color, style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    if (showEndpoints) {
        route.segments.firstOrNull()?.firstOrNull()?.let { drawCircle(endpointColor, strokePx * 1.4f, map(it)) }
        route.segments.lastOrNull()?.lastOrNull()?.let { drawCircle(endpointColor, strokePx * 1.4f, map(it)) }
    }
}
