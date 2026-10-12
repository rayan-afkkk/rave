package com.rave.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rave.app.ui.theme.PillShape

/** A single vertical pill bar filled from the bottom (insights design). */
@Composable
fun PillBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 56.dp, width: Dp = 14.dp) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "pillBar")
    Box(
        modifier.width(width).height(height).clip(PillShape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (animated > 0f) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(animated.coerceAtLeast(0.18f)).clip(PillShape).background(color))
        }
    }
}

/** Row of pill bars with labels under each (e.g. Mon–Sun). */
@Composable
fun PillBarRow(values: List<Float>, labels: List<String>, color: Color, modifier: Modifier = Modifier, description: String = "") {
    Row(
        modifier.fillMaxWidth().semantics { if (description.isNotEmpty()) contentDescription = description },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        values.forEachIndexed { i, v ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PillBar(v, color)
                Spacer(Modifier.height(4.dp))
                Text(labels.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Simple rounded bar chart for km per week / month. */
@Composable
fun BarChart(values: List<Double>, labels: List<String>, color: Color, modifier: Modifier = Modifier, height: Dp = 140.dp, description: String = "") {
    val max = (values.maxOrNull() ?: 0.0).coerceAtLeast(0.001)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier.semantics { if (description.isNotEmpty()) contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val n = values.size.coerceAtLeast(1)
            val slot = size.width / n
            val barW = slot * 0.55f
            values.forEachIndexed { i, v ->
                val h = (v / max).toFloat() * size.height
                val x = i * slot + (slot - barW) / 2
                drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), CornerRadius(barW / 2))
                if (h > 0) drawRoundRect(color, Offset(x, size.height - h), Size(barW, h), CornerRadius(barW / 2))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            labels.forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
            }
        }
    }
}

/**
 * Month heatmap: 7 columns (Mon–Sun), cells coloured by completion (0..1).
 * [leadingBlanks] = weekday offset of the 1st of the month.
 */
@Composable
fun MonthHeatmap(values: List<Float?>, leadingBlanks: Int, weekdayLabels: List<String>, modifier: Modifier = Modifier) {
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val low = MaterialTheme.colorScheme.primaryContainer
    val high = MaterialTheme.colorScheme.primary
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            weekdayLabels.forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(Modifier.height(6.dp))
        val cells = List(leadingBlanks) { null as Float? to false } + values.map { it to true }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                (0 until 7).forEach { i ->
                    val cell = week.getOrNull(i)
                    Box(Modifier.weight(1f).padding(3.dp).aspectRatio(1f)) {
                        if (cell != null && cell.second) {
                            val v = cell.first
                            val color = when {
                                v == null -> empty.copy(alpha = 0.4f)
                                v <= 0f -> empty
                                else -> lerp(low, high, v.coerceIn(0f, 1f))
                            }
                            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.extraSmall).background(color))
                        }
                    }
                }
            }
        }
    }
}

/** Minimal multi-series line chart (e.g. me vs leader, cumulative per day). */
@Composable
fun LineChart(series: List<Pair<List<Double>, Color>>, modifier: Modifier = Modifier, height: Dp = 120.dp, description: String = "") {
    val max = series.flatMap { it.first }.maxOrNull()?.coerceAtLeast(0.001) ?: 1.0
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxWidth().height(height).semantics { if (description.isNotEmpty()) contentDescription = description }) {
        for (i in 0..3) {
            val y = size.height * i / 3f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        series.forEach { (values, color) ->
            if (values.size < 2) {
                values.firstOrNull()?.let { drawCircle(color, 5f, Offset(0f, size.height - (it / max).toFloat() * size.height)) }
                return@forEach
            }
            val step = size.width / (values.size - 1)
            val path = androidx.compose.ui.graphics.Path()
            values.forEachIndexed { i, v ->
                val p = Offset(i * step, size.height - (v / max).toFloat() * size.height)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
    }
}
