package com.rave.app.ui.tracking

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rave.app.R
import com.rave.app.domain.model.LatLngPoint
import com.rave.app.domain.tracking.GeoMath
import com.rave.app.ui.theme.RaveTheme

/**
 * Tile-free live view for free users: the route so far, the planned guide route and the
 * current position on a plain themed background, auto-fitted into the free space between
 * [topInset] and [bottomInset]. No map tiles are requested.
 */
@Composable
fun LiveRouteCanvas(
    segments: List<List<LatLngPoint>>,
    current: LatLngPoint?,
    start: LatLngPoint?,
    guide: List<LatLngPoint>,
    topInset: Dp,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    val c = RaveTheme.colors
    val scheme = MaterialTheme.colorScheme
    val routeColor = c.tileOrange.container
    val dotColor = scheme.onSurface
    val ringColor = scheme.surface
    val guideColor = scheme.onSurfaceVariant.copy(alpha = 0.55f)

    val ref = start ?: segments.firstOrNull { it.isNotEmpty() }?.first() ?: current ?: guide.firstOrNull()
    val scene = remember(segments, guide, current, start, ref) { ref?.let { buildScene(it, segments, guide, current, start) } }

    // Animate the framing so the view eases out as the route grows instead of jumping.
    val cx by animateFloatAsState(scene?.centerX ?: 0f, tween(FRAME_MS), label = "cx")
    val cy by animateFloatAsState(scene?.centerY ?: 0f, tween(FRAME_MS), label = "cy")
    val spanX by animateFloatAsState(scene?.spanX ?: MIN_SPAN_M, tween(FRAME_MS), label = "spanX")
    val spanY by animateFloatAsState(scene?.spanY ?: MIN_SPAN_M, tween(FRAME_MS), label = "spanY")

    val pulse = rememberInfiniteTransition(label = "livePulse")
    val pulseT by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "pulseT")
    val description = stringResource(R.string.pb_live_route_desc)

    Canvas(modifier.background(scheme.surfaceContainer).semantics { contentDescription = description }) {
        val s = scene ?: return@Canvas
        val side = 32.dp.toPx()
        val top = topInset.toPx()
        val bottom = bottomInset.toPx()
        val availW = (size.width - side * 2).coerceAtLeast(1f)
        val availH = (size.height - top - bottom).coerceAtLeast(size.height * 0.25f)
        val originY = if (size.height - top - bottom < size.height * 0.25f) (size.height - availH) / 2f else top
        val scale = minOf(availW / spanX, availH / spanY)
        fun toScreen(o: Offset) = Offset(side + availW / 2f + (o.x - cx) * scale, originY + availH / 2f + (o.y - cy) * scale)

        val stroke = 7.dp.toPx()
        if (s.guide.size >= 2) {
            drawPath(
                pathOf(s.guide) { toScreen(it) }, guideColor,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 9.dp.toPx()))),
            )
        }
        s.segments.filter { it.size >= 2 }.forEach { seg ->
            drawPath(pathOf(seg) { toScreen(it) }, routeColor, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        s.start?.let { p ->
            val o = toScreen(p)
            drawCircle(dotColor, 7.dp.toPx(), o)
            drawCircle(ringColor, 4.dp.toPx(), o)
        }
        s.current?.let { p ->
            val o = toScreen(p)
            drawCircle(routeColor.copy(alpha = 0.45f * (1f - pulseT)), 8.dp.toPx() + 18.dp.toPx() * pulseT, o)
            drawCircle(ringColor, 10.dp.toPx(), o)
            drawCircle(dotColor, 7.dp.toPx(), o)
        }
    }
}

private class LiveScene(
    val segments: List<List<Offset>>,
    val guide: List<Offset>,
    val current: Offset?,
    val start: Offset?,
    val centerX: Float,
    val centerY: Float,
    val spanX: Float,
    val spanY: Float,
)

private fun buildScene(
    ref: LatLngPoint,
    segments: List<List<LatLngPoint>>,
    guide: List<LatLngPoint>,
    current: LatLngPoint?,
    start: LatLngPoint?,
): LiveScene {
    fun local(p: LatLngPoint): Offset {
        val (x, y) = GeoMath.toLocalMeters(p.lat, p.lng, ref.lat, ref.lng)
        return Offset(x.toFloat(), -y.toFloat())
    }
    val segs = segments.map { seg -> seg.map(::local) }
    val g = guide.map(::local)
    val cur = current?.let(::local)
    val st = start?.let(::local)
    var minX = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    fun include(o: Offset) {
        if (o.x < minX) minX = o.x
        if (o.x > maxX) maxX = o.x
        if (o.y < minY) minY = o.y
        if (o.y > maxY) maxY = o.y
    }
    segs.forEach { it.forEach(::include) }
    g.forEach(::include)
    cur?.let(::include)
    st?.let(::include)
    if (minX > maxX) { minX = 0f; maxX = 0f; minY = 0f; maxY = 0f }
    return LiveScene(
        segs, g, cur, st,
        centerX = (minX + maxX) / 2f, centerY = (minY + maxY) / 2f,
        spanX = (maxX - minX).coerceAtLeast(MIN_SPAN_M), spanY = (maxY - minY).coerceAtLeast(MIN_SPAN_M),
    )
}

private inline fun pathOf(points: List<Offset>, toScreen: (Offset) -> Offset): Path {
    val path = Path()
    points.forEachIndexed { i, o ->
        val p = toScreen(o)
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    return path
}

/** Smallest area shown (metres), so a fresh walk isn't zoomed in to a few steps. */
private const val MIN_SPAN_M = 160f
private const val FRAME_MS = 600
