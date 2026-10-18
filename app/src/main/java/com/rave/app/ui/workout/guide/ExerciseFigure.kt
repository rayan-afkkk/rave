package com.rave.app.ui.workout.guide

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import com.rave.app.R
import com.rave.app.ui.theme.FigureBody
import com.rave.app.ui.theme.FigureBodyFar
import com.rave.app.ui.theme.FigureDetail
import com.rave.app.ui.theme.FigureFloor
import com.rave.app.ui.theme.FigureGear
import com.rave.app.ui.theme.FigureHub
import com.rave.app.ui.theme.FigureHubLight
import com.rave.app.ui.theme.FigureMuscle
import com.rave.app.ui.theme.FigureMuscleFar
import com.rave.app.ui.theme.FigurePad
import com.rave.app.ui.theme.FigurePlate
import com.rave.app.ui.theme.FigurePlateRim
import com.rave.app.ui.theme.FigureSteel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign

/** Muscles a move works. Each is drawn as a glowing patch on one body segment. */
enum class Muscle(
    @StringRes val label: Int,
    internal val part: Part,
    /** +1 front of the segment, -1 back, 0 centred. */
    internal val side: Float,
    /** Position along the segment (0 = start joint, 1 = end joint). */
    internal val t: Float,
    internal val length: Float,
    internal val depth: Float,
) {
    QUAD(R.string.guide_m_quads, Part.THIGH, 1f, 0.5f, 0.8f, 1f),
    HAM(R.string.guide_m_hamstrings, Part.THIGH, -1f, 0.5f, 0.7f, 1f),
    GLUTE(R.string.guide_m_glutes, Part.THIGH, -1f, 0.06f, 0.4f, 1.1f),
    CALF(R.string.guide_m_calves, Part.SHIN, -1f, 0.32f, 0.55f, 1f),
    CHEST(R.string.guide_m_chest, Part.TORSO, 1f, 0.22f, 0.34f, 0.9f),
    ABS(R.string.guide_m_abs, Part.TORSO, 1f, 0.62f, 0.46f, 0.6f),
    LAT(R.string.guide_m_lats, Part.TORSO, -1f, 0.4f, 0.5f, 0.8f),
    BACK(R.string.guide_m_lower_back, Part.TORSO, -1f, 0.7f, 0.6f, 0.6f),
    DELT(R.string.guide_m_shoulders, Part.UPPER_ARM, 0f, 0.06f, 0.36f, 1.2f),
    BICEPS(R.string.guide_m_biceps, Part.UPPER_ARM, 1f, 0.56f, 0.6f, 1f),
    TRICEPS(R.string.guide_m_triceps, Part.UPPER_ARM, -1f, 0.48f, 0.65f, 1f),
    FOREARM(R.string.guide_m_forearms, Part.FOREARM, 1f, 0.3f, 0.6f, 1.1f),
}

internal enum class Part { THIGH, SHIN, FOOT, TORSO, UPPER_ARM, FOREARM, PELVIS, NECK }

enum class Gear { BENCH, INCLINE, PULLBAR, DIPBARS, MAT, STEP, CABLE, PULLDOWN, LEGPRESS, PLATE, PLATE_BACK, DB, KB }

/**
 * One exercise animation: keyframe poses (see [J] for the joint layout) played in a loop. Two frames
 * play there-and-back; more frames play in order.
 */
class Move(
    val muscles: List<Muscle>,
    val gear: List<Gear>,
    val flip: Float,
    /** Seen from the front instead of the side (lateral raise). */
    val front: Boolean,
    val frames: List<FloatArray>,
) {
    /** One loop, in ms. */
    val durationMs: Int get() = if (frames.size == 2) 2400 else frames.size * 900

    /** The loop position that shows the move best in a still. */
    val showcaseT: Float get() = if (frames.size == 2) 0.5f else 2f / frames.size
}

/** Joint indices into a pose array (x at 2i, y at 2i + 1). */
internal object J {
    const val HEAD = 0; const val NECK = 1; const val HIP = 2
    const val KNEE = 3; const val ANKLE = 4; const val KNEE2 = 5; const val ANKLE2 = 6
    const val ELBOW = 7; const val WRIST = 8; const val ELBOW2 = 9; const val WRIST2 = 10
    const val SH = 11; const val SH2 = 12; const val HP = 13; const val HP2 = 14; const val TOE = 15; const val TOE2 = 16
    const val COUNT = 17
}

/**
 * Draws [move] as a black silhouette with the worked muscles glowing. [animate] loops it; otherwise a
 * still at the move's showcase pose.
 */
@Composable
fun ExerciseFigure(move: Move, modifier: Modifier = Modifier, animate: Boolean = true, glow: Boolean = true) {
    val t = if (animate) {
        val loop = rememberInfiniteTransition(label = "exercise")
        val v by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(move.durationMs, easing = LinearEasing), RepeatMode.Restart), label = "t")
        v
    } else move.showcaseT
    val pose = remember(move) { FloatArray(J.COUNT * 2) }
    Canvas(modifier) {
        poseAt(move, t, pose)
        drawMove(move, pose, glow)
    }
}

private fun ease(f: Float) = if (f < 0.5f) 4 * f * f * f else 1 - (-2 * f + 2).pow(3) / 2

internal fun poseAt(move: Move, t: Float, out: FloatArray) {
    val frames = move.frames
    if (frames.size == 1) { frames[0].copyInto(out); return }
    val cycle = frames.size
    val x = (t - floor(t)) * cycle
    val i = floor(x).toInt().coerceIn(0, cycle - 1)
    val e = ease(x - i)
    val a = frames[i]
    val b = frames[(i + 1) % frames.size]
    for (k in out.indices) {
        val av = if (a[k].isNaN()) b[k] else a[k]
        val bv = if (b[k].isNaN()) a[k] else b[k]
        out[k] = av + (bv - av) * e
    }
}

// ---- geometry ----

private fun FloatArray.j(i: Int) = Offset(this[2 * i], this[2 * i + 1])
private fun FloatArray.has(i: Int) = !this[2 * i].isNaN()
private fun nrm(v: Offset): Offset {
    val l = v.getDistance()
    return if (l > 0f) Offset(v.x / l, v.y / l) else Offset(0f, 1f)
}
private fun deg(rad: Float) = (rad * 180f / PI).toFloat()

/** A tapered body segment with a muscle bulge on its front and back edges, as one closed outline. */
private class Seg(val path: Path, val p: Offset, val q: Offset, val w1: Float, val w2: Float, val f: Offset)

private fun seg(p: Offset, q: Offset, w1: Float, w2: Float, fb: Float, fT: Float, bb: Float, bT: Float, flip: Float): Seg {
    val d = nrm(q - p)
    val f = Offset(d.y * flip, -d.x * flip)
    fun pt(t: Float, side: Float, extra: Float): Offset {
        val w = (w1 + (w2 - w1) * t) / 2f + extra
        return lerp(p, q, t) + f * (w * side)
    }
    val af = atan2(f.y, f.x)
    var dl = atan2(d.y, d.x) - af
    while (dl > PI) dl -= (2 * PI).toFloat()
    while (dl < -PI) dl += (2 * PI).toFloat()
    val sweep = if (dl > 0) 180f else -180f
    val path = Path().apply {
        val a = pt(0f, 1f, 0f); moveTo(a.x, a.y)
        val c1 = pt(fT, 1f, fb * 2); val b = pt(1f, 1f, 0f); quadraticTo(c1.x, c1.y, b.x, b.y)
        arcTo(Rect(q, w2 / 2f), deg(af), sweep, false)
        val c2 = pt(bT, -1f, bb * 2); val e = pt(0f, -1f, 0f); quadraticTo(c2.x, c2.y, e.x, e.y)
        arcTo(Rect(p, w1 / 2f), deg(af) + 180f, sweep, false)
        close()
    }
    return Seg(path, p, q, w1, w2, f)
}

private fun DrawScope.muscleSpot(g: Seg, m: Muscle, color: Color, grow: Float) {
    val w = (g.w1 + (g.w2 - g.w1) * m.t) / 2f
    val d = nrm(g.q - g.p)
    val center = lerp(g.p, g.q, m.t) + g.f * (w * m.side)
    val rx = (g.q - g.p).getDistance() * m.length / 2f * grow
    val ry = w * m.depth * grow
    if (rx <= 0f || ry <= 0f) return
    withTransform({
        translate(center.x, center.y)
        rotate(deg(atan2(d.y, d.x)), Offset.Zero)
        scale(rx, ry, Offset.Zero)
    }) {
        drawCircle(
            Brush.radialGradient(0f to color, 0.62f to color, 1f to color.copy(alpha = 0f), center = Offset.Zero, radius = 1f),
            radius = 1f, center = Offset.Zero,
        )
    }
}

private fun Path.curve(a: Offset, b: Offset, c: Offset? = null) {
    moveTo(a.x, a.y)
    if (c == null) lineTo(b.x, b.y) else quadraticTo(b.x, b.y, c.x, c.y)
}

/** Faint lines hinting at the muscle shapes inside the silhouette. */
private fun detailLines(g: Seg, part: Part): Path? {
    fun m(t: Float) = lerp(g.p, g.q, t)
    fun o(p: Offset, k: Float) = p + g.f * k
    val p = Path()
    when (part) {
        Part.THIGH -> { p.curve(o(m(.62f), 4f), o(m(.78f), 9f), o(m(.95f), 5f)); p.curve(o(m(.3f), -2f), o(m(.55f), 1f), o(m(.85f), -1f)) }
        Part.SHIN -> p.curve(o(m(.12f), -2f), o(m(.3f), -1f), o(m(.55f), -3f))
        Part.UPPER_ARM -> { p.curve(o(m(.28f), 9f), m(.36f), o(m(.3f), -9f)); p.curve(o(m(.45f), 1f), o(m(.65f), 1.5f), o(m(.88f), 0f)) }
        Part.FOREARM -> p.curve(o(m(.1f), 2f), o(m(.35f), 0f), o(m(.6f), -1f))
        Part.TORSO -> {
            val w = g.w1 / 2
            p.curve(o(m(.36f), w - 14), o(m(.42f), w - 4), o(m(.32f), w + 2))
            for (t in floatArrayOf(.52f, .63f, .74f)) p.curve(o(m(t), w - 7), o(m(t), w + 2))
            p.curve(o(m(.48f), w - 5), o(m(.82f), w - 6))
            p.curve(o(m(.3f), -w + 5), o(m(.5f), -w + 9), o(m(.72f), -w + 4))
        }
        else -> return null
    }
    return p
}

private class Piece(val seg: Seg, val part: Part, val far: Boolean)

private fun DrawScope.drawMove(move: Move, P: FloatArray, glow: Boolean) {
    val s = min(size.width / 320f, size.height / 300f)
    withTransform({
        translate(size.width / 2f, size.height / 2f)
        scale(s, s, Offset.Zero)
        translate(-150f, -116f)
    }) {
        drawRect(FigureFloor.copy(alpha = 0.12f), Offset(-100f, 256f), Size(500f, 4f))
        val pieces = buildPieces(move, P)
        if (glow) {
            for (pc in pieces) for (m in move.muscles) if (m.part == pc.part) muscleSpot(pc.seg, m, FigureMuscle.copy(alpha = 0.4f), 1.35f)
        }
        drawGearBehind(move)
        drawBody(move, P, pieces)
        drawGearInFront(move, P)
    }
}

private fun buildPieces(move: Move, P: FloatArray): List<Piece> {
    val flip = move.flip
    val neck = P.j(J.NECK); val hip = P.j(J.HIP)
    fun or(i: Int, fallback: Offset) = if (P.has(i)) P.j(i) else fallback
    fun leg(far: Boolean): List<Piece> {
        val h = or(if (far) J.HP2 else J.HP, hip)
        val k = P.j(if (far) J.KNEE2 else J.KNEE)
        val a = P.j(if (far) J.ANKLE2 else J.ANKLE)
        val sd = nrm(a - k)
        var fw = Offset(sd.y * flip, -sd.x * flip)
        if (abs(sd.y) > 0.6f) fw = Offset(if (fw.x == 0f) 1f else sign(fw.x), 0f)
        val toe = or(if (far) J.TOE2 else J.TOE, a + fw * 15f + sd * 4f)
        val fd = nrm(toe - a)
        return listOf(
            Piece(seg(h, k, 24f, 14f, 3.5f, .4f, 3.5f, .2f, flip), Part.THIGH, far),
            Piece(seg(k, a, 13f, 8f, 1f, .25f, 4.5f, .3f, flip), Part.SHIN, far),
            Piece(seg(a - fd * 3f, toe, 10f, 6f, 0f, .5f, 1f, .5f, flip), Part.FOOT, far),
        )
    }
    fun arm(far: Boolean): List<Piece> {
        val sh = or(if (far) J.SH2 else J.SH, neck)
        val e = P.j(if (far) J.ELBOW2 else J.ELBOW)
        val w = P.j(if (far) J.WRIST2 else J.WRIST)
        return listOf(
            Piece(seg(sh, e, 15f, 9.5f, 2.5f, .58f, 2.5f, .42f, flip), Part.UPPER_ARM, far),
            Piece(seg(e, w, 10f, 6f, 2f, .25f, 1.5f, .3f, flip), Part.FOREARM, far),
        )
    }
    val torso = if (move.front) seg(neck, hip, 40f, 32f, 3.5f, .25f, 3f, .32f, flip) else seg(neck, hip, 31f, 24f, 3.5f, .25f, 3f, .32f, flip)
    val out = ArrayList<Piece>(16)
    out += leg(true); out += arm(true)
    out += Piece(torso, Part.TORSO, false)
    if (move.front) out += Piece(seg(or(J.HP, hip), or(J.HP2, hip), 16f, 16f, 0f, .5f, 0f, .5f, flip), Part.PELVIS, false)
    out += Piece(seg(neck, lerp(neck, P.j(J.HEAD), .62f), 11f, 9.5f, 0f, .5f, 0f, .5f, flip), Part.NECK, false)
    out += leg(false); out += arm(false)
    return out
}

private fun DrawScope.drawBody(move: Move, P: FloatArray, pieces: List<Piece>) {
    val detail = Stroke(1.4f)
    var headDrawn = false
    for (pc in pieces) {
        // The head goes on after the neck, before the near limbs.
        if (!headDrawn && !pc.far && (pc.part == Part.THIGH)) { drawHead(move, P); headDrawn = true }
        drawPath(pc.seg.path, if (pc.far) FigureBodyFar else FigureBody)
        val lit = move.muscles.filter { it.part == pc.part }
        val lines = detailLines(pc.seg, pc.part)
        if (lit.isNotEmpty() || lines != null) {
            clipPath(pc.seg.path) {
                lit.forEach { muscleSpot(pc.seg, it, if (pc.far) FigureMuscleFar else FigureMuscle, 1f) }
                if (lines != null) drawPath(lines, FigureDetail.copy(alpha = 0.13f), style = detail)
            }
        }
        if (pc.part == Part.FOREARM) {
            val w = P.j(if (pc.far) J.WRIST2 else J.WRIST)
            drawCircle(if (pc.far) FigureBodyFar else FigureBody, 5f, w)
        }
    }
    if (!headDrawn) drawHead(move, P)
}

private fun DrawScope.drawHead(move: Move, P: FloatArray) {
    val head = P.j(J.HEAD)
    drawCircle(FigureBody, 12.5f, head)
    if (move.front) return
    val hu = nrm(head - P.j(J.NECK))
    val fw = Offset(-hu.y * move.flip, hu.x * move.flip)
    fun hx(a: Float, b: Float) = head + fw * a + hu * b
    val jaw = Path().apply {
        val s = hx(-3f, -10f); moveTo(s.x, s.y)
        hx(6f, -13f).let { lineTo(it.x, it.y) }
        val c1 = hx(10f, -12f); val e1 = hx(10.5f, -8f); quadraticTo(c1.x, c1.y, e1.x, e1.y)
        hx(11.5f, -3f).let { lineTo(it.x, it.y) }
        val c2 = hx(13f, 1f); val e2 = hx(11.5f, 5f); quadraticTo(c2.x, c2.y, e2.x, e2.y)
        hx(0f, 6f).let { lineTo(it.x, it.y) }
        close()
    }
    drawPath(jaw, FigureBody)
}

// ---- equipment ----

private fun DrawScope.rr(x: Float, y: Float, w: Float, h: Float, r: Float, c: Color) =
    drawRoundRect(c, Offset(x, y), Size(w, h), CornerRadius(r))

private fun DrawScope.plate(c: Offset, r: Float) {
    drawCircle(FigurePlate, r, c)
    drawCircle(FigurePlateRim, r - 6f, c, style = Stroke(3f))
    drawCircle(FigureHub, 6f, c)
    drawCircle(FigureHubLight, 3f, c)
}

private fun DrawScope.dumbbell(c: Offset) {
    rr(c.x - 15f, c.y - 11f, 9f, 22f, 3f, FigurePlate)
    rr(c.x + 6f, c.y - 11f, 9f, 22f, 3f, FigurePlate)
    drawRect(FigureHub, Offset(c.x - 7f, c.y - 2.5f), Size(14f, 5f))
}

private fun DrawScope.drawGearBehind(move: Move) {
    for (g in move.gear) when (g) {
        Gear.BENCH -> {
            drawRect(FigureGear, Offset(56f, 206f), Size(10f, 50f)); drawRect(FigureGear, Offset(178f, 206f), Size(10f, 50f))
            drawRect(FigureGear, Offset(48f, 250f), Size(26f, 6f)); drawRect(FigureGear, Offset(170f, 250f), Size(26f, 6f))
            rr(34f, 194f, 176f, 14f, 7f, FigurePad)
        }
        Gear.INCLINE -> {
            drawRect(FigureGear, Offset(150f, 206f), Size(10f, 50f)); drawRect(FigureGear, Offset(104f, 160f), Size(10f, 96f))
            drawRect(FigureGear, Offset(96f, 250f), Size(74f, 6f))
            drawLine(FigurePad, Offset(166f, 208f), Offset(100f, 146f), 13f, StrokeCap.Round)
            rr(140f, 200f, 60f, 13f, 6f, FigurePad)
        }
        Gear.PULLBAR -> {
            drawRect(FigureGear, Offset(20f, 28f), Size(10f, 236f)); drawRect(FigureGear, Offset(270f, 28f), Size(10f, 236f))
            rr(14f, 22f, 272f, 9f, 4f, FigureSteel)
        }
        Gear.DIPBARS -> {
            drawRect(FigureGear, Offset(96f, 130f), Size(10f, 126f)); drawRect(FigureGear, Offset(200f, 130f), Size(10f, 126f))
            rr(88f, 124f, 130f, 9f, 4f, FigureSteel)
        }
        Gear.MAT -> rr(10f, 252f, 290f, 6f, 3f, FigurePad)
        Gear.STEP -> rr(118f, 240f, 70f, 16f, 4f, FigureGear)
        Gear.CABLE -> { drawRect(FigureGear, Offset(226f, -20f), Size(14f, 276f)); drawCircle(FigureSteel, 7f, Offset(222f, 0f)) }
        Gear.PULLDOWN -> {
            drawRect(FigureGear, Offset(96f, -20f), Size(12f, 276f)); drawRect(FigureGear, Offset(96f, -20f), Size(90f, 10f))
            drawCircle(FigureSteel, 7f, Offset(180f, -4f))
            rr(120f, 206f, 80f, 12f, 6f, FigurePad)
            drawRect(FigureGear, Offset(150f, 218f), Size(10f, 38f)); drawRect(FigureGear, Offset(110f, 250f), Size(90f, 6f))
            rr(196f, 176f, 22f, 12f, 6f, FigurePad)
        }
        Gear.LEGPRESS -> {
            withTransform({ translate(110f, 214f); rotate(deg(-0.72f), Offset.Zero) }) { rr(-80f, 0f, 90f, 13f, 6f, FigurePad) }
            rr(96f, 210f, 60f, 12f, 6f, FigurePad)
            drawRect(FigureGear, Offset(110f, 222f), Size(10f, 34f)); drawRect(FigureGear, Offset(60f, 250f), Size(200f, 6f))
            drawLine(FigureGear, Offset(150f, 250f), Offset(270f, 90f), 8f)
        }
        else -> Unit
    }
}

private fun DrawScope.drawGearInFront(move: Move, P: FloatArray) {
    val wrist = P.j(J.WRIST)
    for (g in move.gear) when (g) {
        Gear.PLATE -> plate(wrist, if (Gear.BENCH in move.gear) 26f else 30f)
        Gear.PLATE_BACK -> plate(P.j(J.NECK) + Offset(-6f, 2f), 30f)
        Gear.DB -> { dumbbell(wrist); if (move.front) dumbbell(P.j(J.WRIST2)) }
        Gear.KB -> {
            drawCircle(FigurePlate, 12f, wrist + Offset(0f, 14f))
            drawArc(FigurePlate, 180f, 180f, false, wrist + Offset(-6f, -4f), Size(12f, 12f), style = Stroke(3.5f))
        }
        Gear.CABLE, Gear.PULLDOWN -> {
            val top = if (g == Gear.CABLE) Offset(222f, 0f) else Offset(180f, -4f)
            drawLine(FigureSteel, top, wrist, 2f)
            drawCircle(FigureSteel, 4f, wrist)
        }
        Gear.LEGPRESS -> {
            val a = P.j(J.TOE); val b = P.j(J.ANKLE)
            val mid = lerp(a, b, .5f); val d = nrm(a - b)
            withTransform({ translate(mid.x + d.y * 6f, mid.y - d.x * 6f); rotate(deg(atan2(d.y, d.x)), Offset.Zero) }) {
                rr(-26f, -5f, 52f, 10f, 4f, FigureGear)
            }
        }
        else -> Unit
    }
}
