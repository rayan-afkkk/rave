package com.rave.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Rounded rectangle with "continuous" (squircle-style) corners: the curve eases into the
 * straight edges instead of meeting them abruptly, so tiles look softer than a plain
 * [androidx.compose.foundation.shape.RoundedCornerShape]. Same maths as Figma's corner smoothing.
 *
 * @param smoothing 0 = ordinary rounded corner, 1 = maximum smoothing (0.6 matches iOS; Rave uses 1 for extra-soft corners).
 */
class SmoothCornerShape(private val radius: Dp, private val smoothing: Float = 1f) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val budget = min(size.width, size.height) / 2f
        val r = min(with(density) { radius.toPx() }, budget)
        if (r <= 0f) return Outline.Rectangle(Rect(Offset.Zero, size))
        val path = Path()
        val w = size.width
        val h = size.height
        // Corners in clockwise order: apex, direction along the incoming edge, direction of the outgoing edge.
        val corners = listOf(
            Triple(Offset(w, 0f), Offset(1f, 0f), Offset(0f, 1f)),   // top-right
            Triple(Offset(w, h), Offset(0f, 1f), Offset(-1f, 0f)),   // bottom-right
            Triple(Offset(0f, h), Offset(-1f, 0f), Offset(0f, -1f)), // bottom-left
            Triple(Offset(0f, 0f), Offset(0f, -1f), Offset(1f, 0f)), // top-left
        )
        val p = params(r, budget)
        corners.forEachIndexed { i, (apex, u, v) ->
            val start = apex - u * p.p
            if (i == 0) path.moveTo(start.x, start.y) else path.lineTo(start.x, start.y)
            val e1 = start + u * (p.a + p.b + p.c) + v * p.d
            path.cubicTo(start + u * p.a, start + u * (p.a + p.b), e1)
            // Circular arc between the two smoothing curves.
            val center = apex - u * r + v * r
            val startAngle = Math.toDegrees(atan2((e1.y - center.y).toDouble(), (e1.x - center.x).toDouble())).toFloat()
            path.arcTo(Rect(center, r), startAngle, p.arcDegrees, false)
            val e2 = e1 + u * p.arcSection + v * p.arcSection
            path.cubicTo(e2 + u * p.d + v * p.c, e2 + u * p.d + v * (p.b + p.c), e2 + u * p.d + v * (p.a + p.b + p.c))
        }
        path.close()
        return Outline.Generic(path)
    }

    private class Params(val a: Float, val b: Float, val c: Float, val d: Float, val p: Float, val arcSection: Float, val arcDegrees: Float)

    private fun params(r: Float, budget: Float): Params {
        var s = smoothing.coerceIn(0f, 1f)
        var p = (1 + s) * r
        s = min(s, budget / r - 1).coerceAtLeast(0f)
        p = min(p, budget)
        val arcMeasure = 90f * (1 - s)
        val arcSection = (sin(rad(arcMeasure / 2)) * r * sqrt(2.0)).toFloat()
        val alpha = (90f - arcMeasure) / 2
        val p3ToP4 = (r * tan(rad(alpha / 2))).toFloat()
        val beta = 45f * s
        val c = (p3ToP4 * cos(rad(beta))).toFloat()
        val d = (c * tan(rad(beta))).toFloat()
        val b = ((p - arcSection - c - d) / 3).coerceAtLeast(0f)
        return Params(a = 2 * b, b = b, c = c, d = d, p = p, arcSection = arcSection, arcDegrees = arcMeasure)
    }

    private fun rad(deg: Float) = Math.toRadians(deg.toDouble())

    private fun Path.cubicTo(c1: Offset, c2: Offset, end: Offset) = cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)

    override fun equals(other: Any?) = other is SmoothCornerShape && other.radius == radius && other.smoothing == smoothing
    override fun hashCode() = radius.hashCode() * 31 + smoothing.hashCode()
}
