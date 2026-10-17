package com.rave.app.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rave.app.R
import com.rave.app.ui.components.RaveLogo
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.theme.TileColor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Motion-graphics illustrations for onboarding. Each one replays from the start whenever its
 * page becomes [active], and is drawn entirely with theme tokens (no image assets).
 */

/** 0 → 1 progress that restarts every time [active] turns true. */
@Composable
private fun rememberIntro(active: Boolean, durationMs: Int, delayMs: Long = 150): Float {
    val p = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            p.snapTo(0f)
            delay(delayMs)
            p.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing))
        } else {
            p.snapTo(0f)
        }
    }
    return p.value
}

/** Maps overall progress to a sub-range: 0 before [start], 1 after [end]. */
private fun Float.segment(start: Float, end: Float): Float = ((this - start) / (end - start)).coerceIn(0f, 1f)

// ---- Page 1: welcome ----

@Composable
fun WelcomeArt(active: Boolean, modifier: Modifier = Modifier) {
    val intro = rememberIntro(active, 1200)
    val loop = rememberInfiniteTransition(label = "welcome")
    val ripple by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "ripple")
    val flicker by loop.animateFloat(0.97f, 1.04f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "breathe")
    val accent = MaterialTheme.colorScheme.primary
    val ring = RaveTheme.colors.tilePeach.container
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val maxR = size.minDimension / 2
            // Expanding ripples behind the logo.
            for (i in 0 until 3) {
                val t = (ripple + i / 3f) % 1f
                drawCircle(accent.copy(alpha = (1 - t) * 0.35f * intro), radius = maxR * (0.35f + t * 0.65f), center = c, style = Stroke(3.dp.toPx()))
            }
            drawCircle(ring, radius = maxR * 0.42f * intro, center = c)
            // Orbiting dots (habits, routes, friends).
            for (i in 0 until 3) {
                val angle = (ripple * 2 * PI + i * 2 * PI / 3).toFloat()
                val r = maxR * 0.62f
                drawCircle(accent.copy(alpha = intro), radius = 6.dp.toPx(), center = Offset(c.x + cos(angle) * r, c.y + sin(angle) * r))
            }
        }
        RaveLogo(
            color = accent,
            progress = intro.segment(0.15f, 1f),
            modifier = Modifier.size(120.dp).graphicsLayer {
                scaleX = flicker; scaleY = flicker
            },
        )
        // Little stat chips pop in around the logo.
        val chips = listOf(
            Triple(R.string.onb_chip_streak, Alignment.TopStart, 0.45f),
            Triple(R.string.onb_chip_walk, Alignment.CenterEnd, 0.6f),
            Triple(R.string.onb_chip_habits, Alignment.BottomStart, 0.75f),
        )
        chips.forEach { (text, align, at) ->
            val pop = intro.segment(at, at + 0.2f)
            Text(
                stringResource(text),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(align).padding(18.dp)
                    .graphicsLayer { alpha = pop; scaleX = 0.6f + 0.4f * pop; scaleY = 0.6f + 0.4f * pop }
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

// ---- Page 2: habits ----

@Composable
fun HabitsArt(active: Boolean, modifier: Modifier = Modifier) {
    val intro = rememberIntro(active, 2600)
    val tiles = RaveTheme.colors
    val rows = listOf(
        Triple(Icons.Rounded.WaterDrop, tiles.tileBlue, 0.25f),
        Triple(Icons.Rounded.SelfImprovement, tiles.tilePink, 0.45f),
        Triple(Icons.Rounded.AutoStories, tiles.tilePeach, 0.65f),
    )
    val done = rows.count { intro >= it.third + 0.08f }
    Column(modifier.padding(horizontal = 28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            val ringProgress = done / rows.size.toFloat()
            val animatedRing = remember { Animatable(0f) }
            LaunchedEffect(ringProgress) { animatedRing.animateTo(ringProgress, spring(stiffness = Spring.StiffnessLow)) }
            val track = MaterialTheme.colorScheme.surface
            val accent = MaterialTheme.colorScheme.primary
            Canvas(Modifier.size(120.dp)) {
                val sw = 12.dp.toPx()
                drawArc(track, 0f, 360f, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(sw))
                drawArc(accent, -90f, 360f * animatedRing.value, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw),
                    style = Stroke(sw, cap = StrokeCap.Round))
            }
            Text("$done/${rows.size}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.height(18.dp))
        rows.forEachIndexed { i, (icon, tile, at) ->
            val slide = intro.segment(i * 0.08f, i * 0.08f + 0.25f)
            val checked = intro >= at + 0.08f
            val bounce = remember { Animatable(1f) }
            LaunchedEffect(checked) {
                if (checked) {
                    bounce.snapTo(0.6f)
                    bounce.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
                }
            }
            Row(
                Modifier
                    .padding(vertical = 5.dp)
                    .fillMaxWidth()
                    .graphicsLayer { alpha = slide; translationX = (1 - slide) * 120f }
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(28.dp).graphicsLayer { scaleX = bounce.value; scaleY = bounce.value }.clip(CircleShape)
                        .background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (checked) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                ArtTile(icon, tile)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.height(10.dp).fillMaxWidth(0.55f + i * 0.1f).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
            }
        }
    }
}

// ---- Page 3: GPS ----

@Composable
fun GpsArt(active: Boolean, modifier: Modifier = Modifier) {
    val intro = rememberIntro(active, 2800)
    val pulse by rememberInfiniteTransition(label = "gpsPulse").animateFloat(0f, 1f, infiniteRepeatable(tween(1300)), label = "pulse")
    val land = RaveTheme.colors.tileGreen.container
    val road = MaterialTheme.colorScheme.surface
    val park = RaveTheme.colors.tileGreen.content.copy(alpha = 0.18f)
    val accent = MaterialTheme.colorScheme.primary
    val start = MaterialTheme.colorScheme.tertiary
    Box(modifier.padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(260.dp).clip(MaterialTheme.shapes.large)) {
            drawRect(land)
            drawRoundRect(park, Offset(size.width * 0.58f, size.height * 0.08f), Size(size.width * 0.34f, size.height * 0.4f), CornerRadius(24f))
            // Street grid slides in.
            val gridShift = (1 - intro.segment(0f, 0.3f)) * 60f
            for (i in 1..4) {
                val y = size.height * i / 5f + gridShift
                drawLine(road, Offset(0f, y), Offset(size.width, y - 30f), strokeWidth = 14f)
                val x = size.width * i / 5f - gridShift
                drawLine(road, Offset(x, 0f), Offset(x - 40f, size.height), strokeWidth = 12f)
            }
            // The route draws itself.
            val path = Path().apply {
                moveTo(size.width * 0.12f, size.height * 0.85f)
                cubicTo(size.width * 0.3f, size.height * 0.7f, size.width * 0.25f, size.height * 0.45f, size.width * 0.45f, size.height * 0.42f)
                cubicTo(size.width * 0.65f, size.height * 0.4f, size.width * 0.6f, size.height * 0.2f, size.width * 0.86f, size.height * 0.16f)
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val drawn = intro.segment(0.25f, 0.95f)
            val partial = Path()
            measure.getSegment(0f, measure.length * drawn, partial, true)
            drawPath(partial, accent, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            val startPos = measure.getPosition(0f)
            drawCircle(start, 7.dp.toPx(), startPos)
            if (drawn > 0f) {
                val head = measure.getPosition(measure.length * drawn)
                drawCircle(accent.copy(alpha = (1 - pulse) * 0.35f), radius = (8 + 18 * pulse).dp.toPx(), center = head)
                drawCircle(road, 9.dp.toPx(), head)
                drawCircle(accent, 6.dp.toPx(), head)
            }
        }
        // Live distance counter.
        val km = 3.42f * intro.segment(0.25f, 0.95f)
        Row(
            Modifier.align(Alignment.BottomStart).padding(14.dp).clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(String.format(Locale.US, "%.2f", km), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(4.dp))
            Text("km", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Rounded.DirectionsRun, null, tint = accent,
            modifier = Modifier.align(Alignment.TopStart).padding(14.dp).size(30.dp).graphicsLayer { alpha = intro.segment(0.1f, 0.3f) },
        )
    }
}

// ---- Page 4: friends & competitions ----

@Composable
fun FriendsArt(active: Boolean, modifier: Modifier = Modifier) {
    val tiles = RaveTheme.colors
    val steps = listOf(Triple(tiles.tileBlue, 0.62f, "2"), Triple(tiles.tilePeach, 1f, "1"), Triple(tiles.tilePink, 0.42f, "3"))
    val heights = steps.map { remember { Animatable(0f) } }
    val drops = steps.map { remember { Animatable(-400f) } }
    LaunchedEffect(active) {
        heights.forEach { it.snapTo(0f) }
        drops.forEach { it.snapTo(-400f) }
        if (!active) return@LaunchedEffect
        coroutineScope {
            // Podium rises (2nd, 1st, 3rd), then avatars drop in with a bounce.
            listOf(1, 0, 2).forEachIndexed { order, i ->
                async {
                    delay(150L + order * 140L)
                    heights[i].animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                }
                async {
                    delay(700L + order * 160L)
                    drops[i].animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                }
            }
        }
    }
    val confettiT by rememberInfiniteTransition(label = "confetti").animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "c")
    val confettiColors = listOf(MaterialTheme.colorScheme.primary, tiles.tileGreen.content, tiles.tilePink.content, tiles.tileBlue.content)
    val showConfetti = drops[1].value > -10f
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        if (showConfetti) {
            Canvas(Modifier.fillMaxSize()) {
                for (i in 0 until 26) {
                    val seed = i * 37.13f
                    val x = (seed * 13f) % size.width
                    val y = ((confettiT + (i % 7) / 7f) % 1f) * size.height
                    rotate(degrees = confettiT * 360f + seed, pivot = Offset(x, y)) {
                        drawRect(confettiColors[i % confettiColors.size], Offset(x, y), Size(10f, 18f))
                    }
                }
            }
        }
        Row(Modifier.padding(bottom = 24.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            steps.forEachIndexed { i, (tile, h, place) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(if (place == "1") 58.dp else 48.dp).graphicsLayer { translationY = drops[i].value }
                            .clip(CircleShape).background(tile.container),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (place == "1") Icon(Icons.Rounded.EmojiEvents, null, tint = tile.content, modifier = Modifier.size(30.dp))
                        else Text(listOf("A", "", "Z")[i], style = MaterialTheme.typography.titleMedium, color = tile.content)
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.width(84.dp).height((150 * h * heights[i].value).dp)
                            .clip(MaterialTheme.shapes.medium.copy(bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp),
                                bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp)))
                            .background(if (place == "1") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Text(place, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp),
                            color = if (place == "1") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

// ---- Page 5: set tracker ----

@Composable
fun WorkoutArt(active: Boolean, modifier: Modifier = Modifier) {
    val intro = rememberIntro(active, 900)
    val lift by rememberInfiniteTransition(label = "lift").animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "lift")
    val rest by rememberInfiniteTransition(label = "rest").animateFloat(1f, 0f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "rest")
    val bar = MaterialTheme.colorScheme.onBackground
    val plate = MaterialTheme.colorScheme.primary
    val plateLight = RaveTheme.colors.tilePeach.content
    val track = MaterialTheme.colorScheme.surface
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Canvas(Modifier.fillMaxWidth().height(170.dp).graphicsLayer { alpha = intro; scaleX = lerp(0.8f, 1f, intro); scaleY = lerp(0.8f, 1f, intro) }) {
            val y = size.height * (0.72f - 0.42f * lift)
            val cx = size.width / 2
            val half = size.width * 0.36f
            drawLine(bar, Offset(cx - half, y), Offset(cx + half, y), strokeWidth = 10f, cap = StrokeCap.Round)
            for (side in listOf(-1f, 1f)) {
                drawRoundRect(plate, Offset(cx + side * half * 0.78f - 16f, y - 52f), Size(32f, 104f), CornerRadius(10f))
                drawRoundRect(plateLight, Offset(cx + side * half * 0.62f - 12f, y - 38f), Size(24f, 76f), CornerRadius(8f))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("60 × 8", "62.5 × 8", "62.5 × 7").forEachIndexed { i, label ->
                val shown = intro >= 0.3f + i * 0.2f
                Box(
                    Modifier.graphicsLayer { alpha = if (shown) 1f else 0.3f }.clip(MaterialTheme.shapes.small)
                        .background(if (shown) RaveTheme.colors.tileGreen.container else MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge,
                        color = if (shown) RaveTheme.colors.tileGreen.content else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Canvas(Modifier.size(40.dp)) {
                val sw = 5.dp.toPx()
                drawArc(track, 0f, 360f, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(sw))
                drawArc(plate, -90f, 360f * rest, false, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun ArtTile(icon: ImageVector, tile: TileColor) {
    Box(Modifier.size(36.dp).clip(MaterialTheme.shapes.small).background(tile.container), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tile.content, modifier = Modifier.size(20.dp))
    }
}

/** Soft drifting blobs behind every page, tinted with the current page's colour. */
@Composable
fun BoxScope.DriftingBlobs(tint: Color) {
    val t by rememberInfiniteTransition(label = "blobs").animateFloat(0f, 1f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "t")
    Canvas(Modifier.matchParentSize()) {
        val a = (t * 2 * PI).toFloat()
        drawCircle(tint.copy(alpha = 0.55f), radius = size.width * 0.55f,
            center = Offset(size.width * (0.15f + 0.08f * cos(a)), size.height * (0.12f + 0.05f * sin(a))))
        drawCircle(tint.copy(alpha = 0.35f), radius = size.width * 0.45f,
            center = Offset(size.width * (0.95f + 0.06f * sin(a)), size.height * (0.55f + 0.06f * cos(a))))
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction
