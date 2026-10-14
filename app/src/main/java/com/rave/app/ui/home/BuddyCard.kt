package com.rave.app.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.rave.app.data.repository.BuddyRepository
import com.rave.app.domain.buddy.BuddyMood
import com.rave.app.domain.buddy.BuddyStage
import com.rave.app.domain.buddy.BuddyState
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.theme.RaveTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BuddyViewModel @Inject constructor(buddy: BuddyRepository) : ViewModel() {
    val state: StateFlow<BuddyState?> = buddy.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** The Rave buddy: grows a level with every bit of activity, mood follows today. */
@Composable
fun BuddyCard(modifier: Modifier = Modifier, viewModel: BuddyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state ?: return
    SectionCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BuddyAvatar(s.stage, s.mood, Modifier.size(60.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.buddy_level, s.level, stringResource(stageName(s.stage))),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.buddy_xp_short, s.xpIntoLevel, s.xpForNextLevel),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(stringResource(moodLine(s.mood)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                val progress by animateFloatAsState(s.levelProgress, tween(900), label = "xp")
                Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(Modifier.fillMaxWidth(progress.coerceAtLeast(0.03f)).height(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                }
            }
        }
    }
}

private fun stageName(stage: BuddyStage) = when (stage) {
    BuddyStage.SPROUT -> R.string.buddy_stage_sprout
    BuddyStage.BUDDY -> R.string.buddy_stage_buddy
    BuddyStage.RUNNER -> R.string.buddy_stage_runner
    BuddyStage.CHAMP -> R.string.buddy_stage_champ
    BuddyStage.LEGEND -> R.string.buddy_stage_legend
}

private fun moodLine(mood: BuddyMood) = when (mood) {
    BuddyMood.PUMPED -> R.string.buddy_mood_pumped
    BuddyMood.HAPPY -> R.string.buddy_mood_happy
    BuddyMood.OKAY -> R.string.buddy_mood_okay
    BuddyMood.SLEEPY -> R.string.buddy_mood_sleepy
}

/**
 * Drawn character (no image assets): an orange blob that bobs and blinks. It gets bigger with
 * each stage and gains an accessory: sprout leaf → headband → medal → crown.
 */
@Composable
fun BuddyAvatar(stage: BuddyStage, mood: BuddyMood, modifier: Modifier = Modifier) {
    val loop = rememberInfiniteTransition(label = "buddy")
    val bobSpeed = if (mood == BuddyMood.SLEEPY) 2600 else if (mood == BuddyMood.PUMPED) 700 else 1400
    val bob by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(bobSpeed, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val blink by loop.animateFloat(
        1f, 1f,
        infiniteRepeatable(keyframes { durationMillis = 3600; 1f at 3300; 0.1f at 3420; 1f at 3540 }),
        label = "blink",
    )
    val zz by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "zz")
    val body = MaterialTheme.colorScheme.primary
    val bc = RaveTheme.colors.buddy
    val face = bc.face
    val cheek = bc.cheek
    val gold = bc.gold
    val leaf = bc.leaf
    val ribbon = bc.ribbon
    val scale = when (stage) {
        BuddyStage.SPROUT -> 0.72f; BuddyStage.BUDDY -> 0.8f; BuddyStage.RUNNER -> 0.88f; BuddyStage.CHAMP -> 0.94f; BuddyStage.LEGEND -> 1f
    }
    Canvas(modifier) {
        val w = size.width
        val r = w * 0.36f * scale
        val cx = w / 2
        val cy = size.height * 0.6f - bob * w * 0.04f
        // Shadow
        drawOval(Color.Black.copy(alpha = 0.08f), topLeft = Offset(cx - r * 0.9f, size.height * 0.9f), size = Size(r * 1.8f, w * 0.06f))
        // Body: squashy blob
        val squash = 1f + (bob - 0.5f) * 0.06f
        drawOval(body, topLeft = Offset(cx - r * squash, cy - r / squash), size = Size(2 * r * squash, 2 * r / squash))
        // Eyes
        val eyeY = cy - r * 0.15f
        val eyeDx = r * 0.36f
        if (mood == BuddyMood.SLEEPY) {
            listOf(-1, 1).forEach { s ->
                drawLine(face, Offset(cx + s * eyeDx - r * 0.12f, eyeY), Offset(cx + s * eyeDx + r * 0.12f, eyeY), strokeWidth = r * 0.08f, cap = StrokeCap.Round)
            }
        } else {
            listOf(-1, 1).forEach { s ->
                drawOval(face, topLeft = Offset(cx + s * eyeDx - r * 0.09f, eyeY - r * 0.13f * blink), size = Size(r * 0.18f, r * 0.26f * blink))
            }
        }
        // Cheeks
        listOf(-1, 1).forEach { s -> drawCircle(cheek.copy(alpha = 0.7f), r * 0.11f, Offset(cx + s * r * 0.58f, cy + r * 0.1f)) }
        // Mouth
        val mouth = Path()
        val my = cy + r * 0.18f
        when (mood) {
            BuddyMood.PUMPED -> {
                mouth.moveTo(cx - r * 0.22f, my); mouth.quadraticTo(cx, my + r * 0.4f, cx + r * 0.22f, my); mouth.close()
                drawPath(mouth, face)
            }
            BuddyMood.HAPPY -> { mouth.moveTo(cx - r * 0.18f, my); mouth.quadraticTo(cx, my + r * 0.22f, cx + r * 0.18f, my); drawPath(mouth, face, style = Stroke(r * 0.07f, cap = StrokeCap.Round)) }
            BuddyMood.OKAY -> drawLine(face, Offset(cx - r * 0.12f, my + r * 0.05f), Offset(cx + r * 0.12f, my + r * 0.05f), strokeWidth = r * 0.07f, cap = StrokeCap.Round)
            BuddyMood.SLEEPY -> drawCircle(face, r * 0.07f, Offset(cx, my + r * 0.08f))
        }
        accessory(stage, cx, cy, r, gold, leaf, ribbon, face)
        if (mood == BuddyMood.SLEEPY) {
            val zy = cy - r * 1.1f - zz * r * 0.5f
            drawZ(Offset(cx + r * 0.9f, zy), r * 0.22f, face.copy(alpha = 1f - zz))
        }
    }
}

private fun DrawScope.accessory(stage: BuddyStage, cx: Float, cy: Float, r: Float, gold: Color, leaf: Color, ribbon: Color, dark: Color) {
    when (stage) {
        BuddyStage.SPROUT -> {
            drawLine(leaf, Offset(cx, cy - r), Offset(cx, cy - r * 1.3f), strokeWidth = r * 0.08f, cap = StrokeCap.Round)
            drawOval(leaf, topLeft = Offset(cx, cy - r * 1.48f), size = Size(r * 0.42f, r * 0.24f))
        }
        BuddyStage.BUDDY -> drawCircle(Color.White.copy(alpha = 0.35f), r * 0.16f, Offset(cx - r * 0.45f, cy - r * 0.5f))
        BuddyStage.RUNNER -> drawArc(Color.White, 200f, 140f, false, topLeft = Offset(cx - r * 0.92f, cy - r * 0.95f),
            size = Size(r * 1.84f, r * 1.6f), style = Stroke(r * 0.14f, cap = StrokeCap.Round))
        BuddyStage.CHAMP -> {
            drawLine(ribbon, Offset(cx - r * 0.2f, cy + r * 0.55f), Offset(cx, cy + r * 0.85f), strokeWidth = r * 0.08f)
            drawLine(ribbon, Offset(cx + r * 0.2f, cy + r * 0.55f), Offset(cx, cy + r * 0.85f), strokeWidth = r * 0.08f)
            drawCircle(gold, r * 0.16f, Offset(cx, cy + r * 0.9f))
        }
        BuddyStage.LEGEND -> {
            val crown = Path().apply {
                val top = cy - r * 1.32f
                val base = cy - r * 0.86f
                moveTo(cx - r * 0.42f, base); lineTo(cx - r * 0.46f, top + r * 0.12f); lineTo(cx - r * 0.2f, top + r * 0.28f)
                lineTo(cx, top); lineTo(cx + r * 0.2f, top + r * 0.28f); lineTo(cx + r * 0.46f, top + r * 0.12f); lineTo(cx + r * 0.42f, base); close()
            }
            drawPath(crown, gold)
            drawCircle(dark.copy(alpha = 0.25f), r * 0.05f, Offset(cx, cy - r * 1.02f))
        }
    }
}

private fun DrawScope.drawZ(at: Offset, s: Float, color: Color) {
    val p = Path().apply { moveTo(at.x, at.y); lineTo(at.x + s, at.y); lineTo(at.x, at.y + s); lineTo(at.x + s, at.y + s) }
    drawPath(p, color, style = Stroke(s * 0.18f, cap = StrokeCap.Round))
}
