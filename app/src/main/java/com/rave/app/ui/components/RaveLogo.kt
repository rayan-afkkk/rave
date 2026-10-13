package com.rave.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

/**
 * The "Momentum" mark: three rising, slanted pills (the weekly bars, always going up).
 * Geometry lives in a 100 x 100 box; [progress] (0..1) grows the pills one after another
 * so the mark can animate in. Matches res/drawable/ic_launcher_foreground.xml.
 */
@Composable
fun RaveLogo(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    progress: Float = 1f,
) {
    Canvas(modifier) {
        val s = size.minDimension / 100f
        val ox = (size.width - 100f * s) / 2f
        val oy = (size.height - 100f * s) / 2f
        fun p(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
        PILLS.forEachIndexed { i, pill ->
            val grow = ((progress - i * 0.18f) / 0.64f).coerceIn(0f, 1f)
            if (grow <= 0f) return@forEachIndexed
            val bottom = p(pill[0], pill[1])
            val top = p(pill[2], pill[3])
            drawLine(
                color = color.copy(alpha = color.alpha * ALPHAS[i]),
                start = bottom,
                end = bottom + (top - bottom) * grow,
                strokeWidth = 15f * s,
                cap = StrokeCap.Round,
            )
        }
    }
}

// Cap centres (bottom x, bottom y, top x, top y) of each pill, skewed by 14 degrees.
private val PILLS = listOf(
    floatArrayOf(23.43f, 72.5f, 25.67f, 63.5f),
    floatArrayOf(44.43f, 72.5f, 50.66f, 47.5f),
    floatArrayOf(65.43f, 72.5f, 76.64f, 27.5f),
)
private val ALPHAS = floatArrayOf(0.55f, 0.8f, 1f)
