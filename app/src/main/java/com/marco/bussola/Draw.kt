package com.marco.bussola

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Utilità comuni
// ---------------------------------------------------------------------------

/** Memorizza i testi già misurati: il disegno viene ripetuto decine di volte al secondo. */
class TextCache(private val tm: TextMeasurer, private val density: Density) {
    private val map = HashMap<String, TextLayoutResult>()

    fun get(
        text: String,
        sizePx: Float,
        weight: FontWeight = FontWeight.Normal,
        italic: Boolean = false,
    ): TextLayoutResult {
        val key = "$text|${sizePx.roundToInt()}|${weight.weight}|$italic"
        return map.getOrPut(key) {
            val sp = with(density) { sizePx.toSp() }
            tm.measure(
                AnnotatedString(text),
                TextStyle(
                    fontSize = sp,
                    fontWeight = weight,
                    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                    color = Color.White,
                ),
            )
        }
    }
}

class DialEnv(val cache: TextCache, val c: AppColors, val data: DialData)

/** Punto a distanza r dal centro, con angolo in gradi misurato in senso orario da "ore 12". */
fun pol(center: Offset, r: Float, deg: Float): Offset {
    val a = Math.toRadians(deg.toDouble())
    return Offset(center.x + r * sin(a).toFloat(), center.y - r * cos(a).toFloat())
}

fun DrawScope.tick(
    center: Offset,
    deg: Float,
    r1: Float,
    r2: Float,
    color: Color,
    width: Float,
    cap: StrokeCap = StrokeCap.Butt,
) {
    drawLine(color, pol(center, r1, deg), pol(center, r2, deg), width, cap)
}

fun DrawScope.triangle(p1: Offset, p2: Offset, p3: Offset, color: Color) {
    val path = Path().apply {
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        close()
    }
    drawPath(path, color)
}

fun DrawScope.textCentered(l: TextLayoutResult, cx: Float, cy: Float, color: Color) {
    drawText(l, color, Offset(cx - l.size.width / 2f, cy - l.size.height / 2f))
}

/** Testo dritto, posizionato sull'angolo dato. */
fun DrawScope.uprightText(l: TextLayoutResult, deg: Float, radius: Float, color: Color, center: Offset) {
    val p = pol(center, radius, deg)
    textCentered(l, p.x, p.y, color)
}

/** Testo ruotato lungo il raggio (la parte alta del testo guarda verso l'esterno). */
fun DrawScope.radialText(l: TextLayoutResult, deg: Float, radius: Float, color: Color, center: Offset) {
    rotate(deg, center) {
        drawText(
            l,
            color,
            Offset(center.x - l.size.width / 2f, center.y - radius - l.size.height / 2f),
        )
    }
}

/** Tre barrette come l'indicatore di segnale: piene in base all'accuratezza. */
fun DrawScope.accuracyBars(accuracy: Int, center: Offset, h: Float, color: Color) {
    val level = accuracyLevel(accuracy)
    val w = h * 0.30f
    val gap = h * 0.16f
    val total = 3 * w + 2 * gap
    val x0 = center.x - total / 2f
    val bottom = center.y + h / 2f
    val heights = floatArrayOf(0.45f, 0.72f, 1f)
    for (i in 0..2) {
        val bh = h * heights[i]
        drawRoundRect(
            color = if (i < level) color else color.copy(alpha = 0.28f),
            topLeft = Offset(x0 + i * (w + gap), bottom - bh),
            size = Size(w, bh),
            cornerRadius = CornerRadius(w * 0.2f),
        )
    }
}

// ---------------------------------------------------------------------------
// 1. Classico (quadrante originale)
// ---------------------------------------------------------------------------
fun DrawScope.drawClassico(e: DialEnv) {
    val c = e.c
    val h = e.data.heading()
    val ctr = center
    val r = min(size.width, size.height) / 2f
    val tickOuter = r * 0.74f

    for (a in 0 until 360 step 2) {
        val major = a % 30 == 0
        val col = when {
            a == 0 -> c.accent
            major -> c.ink
            else -> c.ink.copy(alpha = 0.7f)
        }
        val len = if (major) r * 0.085f else r * 0.05f
        tick(ctr, a - h, tickOuter - len, tickOuter, col, if (major) r * 0.016f else r * 0.007f)
    }
    for (a in 0 until 360 step 30) {
        val l = e.cache.get(a.toString(), r * 0.06f)
        uprightText(l, a - h, r * 0.84f, if (a == 0) c.accent else c.ink, ctr)
    }
    val letters = listOf("N", "E", "S", "O")
    for (i in 0..3) {
        val l = e.cache.get(letters[i], r * 0.105f, FontWeight.Bold)
        uprightText(l, i * 90f - h, r * 0.53f, if (i == 0) c.accent else c.ink, ctr)
    }

    textCentered(e.cache.get("${degInt(h)}°", r * 0.27f, FontWeight.Light), ctr.x, ctr.y - r * 0.02f, c.ink)
    textCentered(e.cache.get(cardinalName(h), r * 0.115f), ctr.x, ctr.y + r * 0.21f, c.ink)

    // lancetta fissa in alto
    triangle(
        Offset(ctr.x, ctr.y - r * 0.95f),
        Offset(ctr.x - r * 0.06f, ctr.y - r * 0.83f),
        Offset(ctr.x + r * 0.06f, ctr.y - r * 0.83f),
        c.accent,
    )
    drawRect(c.accent, Offset(ctr.x - r * 0.018f, ctr.y - r * 0.83f), Size(r * 0.036f, r * 0.15f))
}

// ---------------------------------------------------------------------------
// 2. Difetto: archi colorati, tacche e freccia fissa
// ---------------------------------------------------------------------------
fun DrawScope.drawDifetto(e: DialEnv) {
    val c = e.c
    val h = e.data.heading()
    val ctr = center
    val r = min(size.width, size.height) / 2f

    val arcR = r * 0.88f
    val arcW = r * 0.095f
    for (i in 0..3) {
        val start = i * 90f - h + 13f
        drawArc(
            color = c.accent,
            startAngle = start - 90f,
            sweepAngle = 64f,
            useCenter = false,
            topLeft = Offset(ctr.x - arcR, ctr.y - arcR),
            size = Size(arcR * 2f, arcR * 2f),
            style = Stroke(arcW),
        )
    }
    val letters = listOf("N", "E", "S", "O")
    for (i in 0..3) {
        radialText(e.cache.get(letters[i], r * 0.13f, FontWeight.Medium), i * 90f - h, arcR, c.ink, ctr)
    }

    val tickOuter = r * 0.78f
    for (a in 0 until 360 step 5) {
        val big = a % 45 == 0
        val mid = a % 15 == 0
        val len = if (big) r * 0.13f else if (mid) r * 0.085f else r * 0.05f
        val w = if (big) r * 0.02f else if (mid) r * 0.014f else r * 0.009f
        tick(ctr, a - h, tickOuter - len, tickOuter, c.ink, w)
    }

    // freccia fissa e trattini di riferimento
    triangle(
        Offset(ctr.x, ctr.y - r * 0.60f),
        Offset(ctr.x - r * 0.15f, ctr.y - r * 0.33f),
        Offset(ctr.x + r * 0.15f, ctr.y - r * 0.33f),
        c.accent,
    )
    val dash = r * 0.012f
    drawLine(c.ink, Offset(ctr.x - r * 0.52f, ctr.y), Offset(ctr.x - r * 0.44f, ctr.y), dash)
    drawLine(c.ink, Offset(ctr.x + r * 0.44f, ctr.y), Offset(ctr.x + r * 0.52f, ctr.y), dash)
    drawLine(c.ink, Offset(ctr.x, ctr.y + r * 0.42f), Offset(ctr.x, ctr.y + r * 0.50f), dash)

    textCentered(e.cache.get(fieldText(e.data.field()), r * 0.075f), ctr.x, ctr.y - r * 0.17f, c.ink)
    textCentered(e.cache.get("${degInt(h)}°", r * 0.22f, FontWeight.Light), ctr.x, ctr.y, c.ink)
    accuracyBars(e.data.accuracy(), Offset(ctr.x, ctr.y + r * 0.2f), r * 0.1f, c.ink)
}

// ---------------------------------------------------------------------------
// 3. iPhone: scala, lettere, mirino e bolla di livello
// ---------------------------------------------------------------------------
fun DrawScope.drawIphone(e: DialEnv) {
    val c = e.c
    val h = e.data.heading()
    val ctr = center
    val r = min(size.width, size.height) / 2f
    val tickOuter = r * 0.74f

    for (a in 0 until 360 step 2) {
        val major = a % 30 == 0
        val mid = a % 10 == 0
        val len = if (major) r * 0.10f else if (mid) r * 0.07f else r * 0.045f
        val w = if (major) r * 0.022f else if (mid) r * 0.012f else r * 0.007f
        val col = if (a == 0) c.accent else c.ink.copy(alpha = if (major || mid) 1f else 0.8f)
        tick(ctr, a - h, tickOuter - len, tickOuter, col, w)
    }
    for (a in 0 until 360 step 30) {
        uprightText(e.cache.get(a.toString(), r * 0.075f), a - h, r * 0.88f, c.ink, ctr)
    }
    val letters = listOf("N", "E", "S", "O")
    for (i in 0..3) {
        uprightText(e.cache.get(letters[i], r * 0.15f, FontWeight.Medium), i * 90f - h, r * 0.52f, c.ink, ctr)
    }
    // triangolino del nord
    triangle(
        pol(ctr, r * 0.73f, -h),
        pol(ctr, r * 0.63f, -h - 3.5f),
        pol(ctr, r * 0.63f, -h + 3.5f),
        c.accent,
    )
    // indice fisso in alto
    drawLine(c.ink, Offset(ctr.x, ctr.y - r * 0.84f), Offset(ctr.x, ctr.y - r * 0.70f), r * 0.022f)

    // mirino
    val arm = r * 0.42f
    val thin = r * 0.006f
    drawLine(c.ink.copy(alpha = 0.85f), Offset(ctr.x - arm, ctr.y), Offset(ctr.x + arm, ctr.y), thin)
    drawLine(c.ink.copy(alpha = 0.85f), Offset(ctr.x, ctr.y - arm), Offset(ctr.x, ctr.y + arm), thin)

    // bolla di livello
    val maxOff = r * 0.16f
    val off = Offset(
        (e.data.levelX() / 0.35f).coerceIn(-1f, 1f) * maxOff,
        (-e.data.levelY() / 0.35f).coerceIn(-1f, 1f) * maxOff,
    )
    drawCircle(c.ink.copy(alpha = 0.42f), r * 0.25f, ctr + off)
    val cross = r * 0.035f
    drawLine(c.accent, Offset(ctr.x - cross, ctr.y), Offset(ctr.x + cross, ctr.y), r * 0.01f)
    drawLine(c.accent, Offset(ctr.x, ctr.y - cross), Offset(ctr.x, ctr.y + cross), r * 0.01f)
}

// ---------------------------------------------------------------------------
// 4. Navigatore: lunetta fissa, lettere e ago che ruotano
// ---------------------------------------------------------------------------
fun DrawScope.drawNavigatore(e: DialEnv) {
    val c = e.c
    val h = e.data.heading()
    val ctr = center
    val r = min(size.width, size.height) / 2f
    val tickOuter = r * 0.72f

    // lunetta fissa con 0 in alto
    for (a in 0 until 360 step 3) {
        if (a == 0) {
            tick(ctr, 0f, tickOuter - r * 0.11f, tickOuter, c.accent, r * 0.024f)
            continue
        }
        val major = a % 15 == 0
        tick(
            ctr, a.toFloat(),
            tickOuter - if (major) r * 0.075f else r * 0.04f, tickOuter,
            c.ink.copy(alpha = if (major) 1f else 0.75f),
            if (major) r * 0.012f else r * 0.006f,
        )
    }
    for (a in 0 until 360 step 15) {
        radialText(e.cache.get(a.toString(), r * 0.058f), a.toFloat(), r * 0.84f, if (a == 0) c.accent else c.ink, ctr)
    }

    // lettere cardinali che ruotano
    val letters = listOf("N", "E", "S", "O")
    for (i in 0..3) {
        radialText(
            e.cache.get(letters[i], r * 0.15f, FontWeight.Bold),
            i * 90f - h, r * 0.56f,
            if (i == 0) c.accent else c.ink, ctr,
        )
    }

    // cerchio interno con due tacche fisse
    drawCircle(c.ink.copy(alpha = 0.9f), r * 0.27f, ctr, style = Stroke(r * 0.006f))
    tick(ctr, 45f, r * 0.27f, r * 0.32f, c.ink, r * 0.008f)
    tick(ctr, 225f, r * 0.27f, r * 0.32f, c.ink, r * 0.008f)

    // ago
    val ang = -h
    val len = r * 0.5f
    val half = r * 0.06f
    val tipN = pol(ctr, len, ang)
    val tipS = pol(ctr, len, ang + 180f)
    val left = pol(ctr, half, ang - 90f)
    val right = pol(ctr, half, ang + 90f)
    triangle(tipN, left, ctr, c.accent)
    triangle(tipN, right, ctr, lerp(c.accent, Color.Black, 0.28f))
    triangle(tipS, left, ctr, Color(0xFFF1F1F1))
    triangle(tipS, right, ctr, Color(0xFFBDBDBD))
    drawCircle(c.ink, r * 0.045f, ctr)
    drawCircle(c.bg, r * 0.026f, ctr)
    drawCircle(c.ink, r * 0.01f, ctr)
}

// ---------------------------------------------------------------------------
// 5. Graduato: graduazioni dettagliate
// ---------------------------------------------------------------------------
fun DrawScope.drawGraduato(e: DialEnv) {
    val c = e.c
    val h = e.data.heading()
    val ctr = center
    val r = min(size.width, size.height) / 2f
    val tickOuter = r * 0.80f

    for (a in 0 until 360 step 2) {
        val major = a % 30 == 0
        val mid = a % 10 == 0
        val len = if (major) r * 0.105f else if (mid) r * 0.07f else r * 0.04f
        val w = if (major) r * 0.016f else if (mid) r * 0.010f else r * 0.006f
        val col = if (a == 0) c.accent else c.ink.copy(alpha = if (major || mid) 1f else 0.75f)
        tick(ctr, a - h, tickOuter - len, tickOuter, col, w)
    }
    for (a in 0 until 360 step 30) {
        radialText(e.cache.get(a.toString(), r * 0.07f), a - h, r * 0.9f, c.ink, ctr)
    }
    // riferimenti fissi rossi ai quattro punti
    for (i in 0..3) {
        tick(ctr, i * 90f, r * 0.70f, r * 0.83f, c.accent, r * 0.018f)
    }
    val letters = listOf("N", "E", "S", "O")
    for (i in 0..3) {
        radialText(
            e.cache.get(letters[i], r * 0.15f, FontWeight.Medium),
            i * 90f - h, r * 0.585f,
            if (i == 0) c.accent else c.ink, ctr,
        )
    }

    drawCircle(c.ink.copy(alpha = 0.9f), r * 0.37f, ctr, style = Stroke(r * 0.005f))
    textCentered(e.cache.get(fieldText(e.data.field()), r * 0.065f), ctr.x, ctr.y - r * 0.16f, c.ink)
    textCentered(e.cache.get("${degInt(h)}°", r * 0.20f, FontWeight.Light), ctr.x, ctr.y, c.ink)
    accuracyBars(e.data.accuracy(), Offset(ctr.x, ctr.y + r * 0.18f), r * 0.09f, c.ink)
}

// ---------------------------------------------------------------------------
// 6. Design: anelli argentati e disco centrale chiaro
// ---------------------------------------------------------------------------
fun DrawScope.drawDesign(e: DialEnv) {
    val c = e.c
    val h = e.data.heading()
    val ctr = center
    val r = min(size.width, size.height) / 2f
    val silver = Color(0xFFCFD8DC)
    val dark = Color(0xFF263238)

    drawCircle(silver, r * 0.935f, ctr, style = Stroke(r * 0.065f))
    drawCircle(c.ink, r * 0.835f, ctr, style = Stroke(r * 0.006f))

    val cardinals = listOf("N", "E", "S", "O")
    for (i in 0..3) {
        radialText(e.cache.get(cardinals[i], r * 0.14f, FontWeight.Bold), i * 90f - h, r * 0.735f, c.accent, ctr)
    }
    val inter = listOf("NE", "SE", "SO", "NO")
    for (i in 0..3) {
        radialText(e.cache.get(inter[i], r * 0.085f, FontWeight.Medium), 45f + i * 90f - h, r * 0.735f, c.ink, ctr)
    }
    for (a in 15 until 360 step 15) {
        if (a % 45 == 0) continue
        drawCircle(c.ink, r * 0.012f, pol(ctr, r * 0.735f, a - h))
    }

    // alone colorato e dischi centrali
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(c.accent.copy(alpha = 0.55f), Color.Transparent),
            center = ctr,
            radius = r * 0.60f,
        ),
        radius = r * 0.60f,
        center = ctr,
    )
    drawCircle(Color(0xFFB0BEC5), r * 0.47f, ctr)
    drawCircle(Color(0xFFECEFF1), r * 0.415f, ctr)

    // indicatori fissi
    triangle(
        Offset(ctr.x, ctr.y - r * 0.80f),
        Offset(ctr.x - r * 0.055f, ctr.y - r * 0.97f),
        Offset(ctr.x + r * 0.055f, ctr.y - r * 0.97f),
        c.accent,
    )
    triangle(
        Offset(ctr.x, ctr.y - r * 0.60f),
        Offset(ctr.x - r * 0.03f, ctr.y - r * 0.54f),
        Offset(ctr.x + r * 0.03f, ctr.y - r * 0.54f),
        c.accent,
    )

    textCentered(e.cache.get(fieldText(e.data.field()), r * 0.07f), ctr.x, ctr.y - r * 0.2f, dark)
    textCentered(e.cache.get("${degInt(h)}°", r * 0.19f, FontWeight.Medium), ctr.x, ctr.y, dark)
    accuracyBars(e.data.accuracy(), Offset(ctr.x, ctr.y + r * 0.2f), r * 0.09f, dark)
}

// ---------------------------------------------------------------------------
// 7. Leggibile: lettere grandi e alto contrasto
// ---------------------------------------------------------------------------
fun DrawScope.drawLeggibile(e: DialEnv) {
    val c = e.c
    val h = e.data.heading()
    val ctr = center
    val r = min(size.width, size.height) / 2f

    val cardinals = listOf("N", "E", "S", "O")
    for (i in 0..3) {
        radialText(
            e.cache.get(cardinals[i], r * 0.17f, FontWeight.ExtraBold),
            i * 90f - h, r * 0.89f,
            if (i == 0) c.accent else c.ink, ctr,
        )
    }
    val inter = listOf("NE", "SE", "SO", "NO")
    for (i in 0..3) {
        radialText(e.cache.get(inter[i], r * 0.11f, FontWeight.ExtraBold), 45f + i * 90f - h, r * 0.89f, c.ink, ctr)
    }
    for (a in 30 until 360 step 30) {
        if (a % 45 == 0 && a % 90 != 0) continue
        radialText(e.cache.get(a.toString(), r * 0.058f), a - h, r * 0.77f, c.ink.copy(alpha = 0.85f), ctr)
    }
    for (a in 0 until 360 step 15) {
        drawCircle(c.ink, r * 0.012f, pol(ctr, r * 0.68f, a - h))
    }
    // triangolo del nord
    triangle(
        pol(ctr, r * 0.80f, -h),
        pol(ctr, r * 0.69f, -h - 4f),
        pol(ctr, r * 0.69f, -h + 4f),
        c.accent,
    )
    // freccia fissa
    triangle(
        Offset(ctr.x, ctr.y - r * 0.52f),
        Offset(ctr.x - r * 0.06f, ctr.y - r * 0.40f),
        Offset(ctr.x + r * 0.06f, ctr.y - r * 0.40f),
        c.ink,
    )

    textCentered(e.cache.get(fieldText(e.data.field()), r * 0.07f), ctr.x, ctr.y - r * 0.17f, c.ink)
    textCentered(e.cache.get("${degInt(h)}°", r * 0.2f, FontWeight.Medium), ctr.x, ctr.y + r * 0.01f, c.accent)
    accuracyBars(e.data.accuracy(), Offset(ctr.x, ctr.y + r * 0.21f), r * 0.09f, c.ink)
}

// ---------------------------------------------------------------------------
// Blocco direzione: linea sulla direzione bloccata e fascia rossa di scostamento
// ---------------------------------------------------------------------------
val LockRed = Color(0xFFFF453A)
val LockGreen = Color(0xFF30D158)

/** Rosso se la direzione si scosta, verde quando si è in rotta (entro 1,5°). */
fun lockColor(diff: Float): Color = if (kotlin.math.abs(diff) <= 1.5f) LockGreen else LockRed

/** Differenza con segno tra due direzioni, nell'intervallo (-180, 180]. */
fun signedDiff(target: Float, from: Float): Float = ((target - from) % 360f + 540f) % 360f - 180f

fun DrawScope.drawLockOverlay(e: DialEnv) {
    val locked = e.data.locked()
    if (locked.isNaN()) return
    val h = e.data.heading()
    val ctr = center
    val r = min(size.width, size.height) / 2f
    val d = signedDiff(locked, h)
    val col = lockColor(d)

    // settore pieno tra la freccia in alto e la direzione bloccata
    val arcR = r * 0.47f
    if (kotlin.math.abs(d) > 0.3f) {
        drawArc(
            color = col.copy(alpha = 0.16f),
            startAngle = -90f,
            sweepAngle = d,
            useCenter = true,
            topLeft = Offset(ctr.x - arcR, ctr.y - arcR),
            size = Size(arcR * 2f, arcR * 2f),
        )
        drawArc(
            color = col,
            startAngle = -90f,
            sweepAngle = d,
            useCenter = false,
            topLeft = Offset(ctr.x - arcR, ctr.y - arcR),
            size = Size(arcR * 2f, arcR * 2f),
            style = Stroke(r * 0.04f, cap = StrokeCap.Round),
        )
    }

    // linea con alone morbido
    tick(ctr, d, r * 0.40f, r * 0.97f, col.copy(alpha = 0.20f), r * 0.07f, StrokeCap.Round)
    tick(ctr, d, r * 0.40f, r * 0.97f, col, r * 0.022f, StrokeCap.Round)
    drawCircle(col, r * 0.03f, pol(ctr, r * 0.40f, d))

    // segnalino sul bordo, rivolto verso il centro
    triangle(
        pol(ctr, r * 0.875f, d),
        pol(ctr, r * 0.985f, d - 3.6f),
        pol(ctr, r * 0.985f, d + 3.6f),
        col,
    )
}
