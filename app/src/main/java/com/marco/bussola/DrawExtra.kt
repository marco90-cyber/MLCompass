package com.marco.bussola

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.min

// ---------------------------------------------------------------------------
// 8. Radar: anelli concentrici e settore che si dissolve
// ---------------------------------------------------------------------------
fun DrawScope.drawRadar(e: DialEnv) {
    val c = e.c
    val h = e.data.heading()
    val ctr = center
    val r = min(size.width, size.height) / 2f

    // settore luminoso che sfuma dietro la linea di mira
    val outer = r * 0.94f
    for (i in 0 until 16) {
        drawArc(
            color = c.accent.copy(alpha = 0.34f * (1f - i / 16f)),
            startAngle = -90f - (i + 1) * 5f,
            sweepAngle = 5.6f,
            useCenter = true,
            topLeft = Offset(ctr.x - outer, ctr.y - outer),
            size = Size(outer * 2f, outer * 2f),
        )
    }
    for (k in listOf(0.34f, 0.50f, 0.66f)) {
        drawCircle(c.ink.copy(alpha = 0.18f), r * k, ctr, style = Stroke(r * 0.006f))
    }
    drawCircle(c.ink.copy(alpha = 0.55f), outer, ctr, style = Stroke(r * 0.012f))
    drawLine(c.ink.copy(alpha = 0.16f), Offset(ctr.x - outer, ctr.y), Offset(ctr.x + outer, ctr.y), r * 0.006f)
    drawLine(c.ink.copy(alpha = 0.16f), Offset(ctr.x, ctr.y - outer), Offset(ctr.x, ctr.y + outer), r * 0.006f)

    for (a in 0 until 360 step 5) {
        val major = a % 30 == 0
        val len = if (major) r * 0.07f else r * 0.035f
        tick(ctr, a - h, outer - len, outer, c.ink, if (major) r * 0.014f else r * 0.008f)
    }
    val letters = listOf("N", "E", "S", "O")
    for (i in 0..3) {
        radialText(
            e.cache.get(letters[i], r * 0.12f, FontWeight.Bold),
            i * 90f - h, r * 0.80f,
            if (i == 0) c.accent else c.ink, ctr,
        )
    }
    // linea di mira
    drawLine(c.accent, ctr, Offset(ctr.x, ctr.y - outer), r * 0.014f, StrokeCap.Round)

    drawCircle(c.card, r * 0.27f, ctr)
    drawCircle(c.accent, r * 0.27f, ctr, style = Stroke(r * 0.01f))
    textCentered(e.cache.get("${degInt(h)}°", r * 0.17f, FontWeight.Medium), ctr.x, ctr.y - r * 0.03f, c.ink)
    accuracyBars(e.data.accuracy(), Offset(ctr.x, ctr.y + r * 0.13f), r * 0.07f, c.ink)
}

// ---------------------------------------------------------------------------
// 9. Minimale: punti sottili e gradi grandi
// ---------------------------------------------------------------------------
fun DrawScope.drawMinimale(e: DialEnv) {
    val c = e.c
    val h = e.data.heading()
    val ctr = center
    val r = min(size.width, size.height) / 2f

    drawCircle(c.ink.copy(alpha = 0.22f), r * 0.96f, ctr, style = Stroke(r * 0.005f))
    for (a in 0 until 360 step 10) {
        val major = a % 30 == 0
        drawCircle(
            if (major) c.ink else c.ink.copy(alpha = 0.45f),
            if (major) r * 0.016f else r * 0.009f,
            pol(ctr, r * 0.88f, a - h),
        )
    }
    val letters = listOf("N", "E", "S", "O")
    for (i in 0..3) {
        uprightText(
            e.cache.get(letters[i], r * 0.11f, FontWeight.Light),
            i * 90f - h, r * 0.72f,
            if (i == 0) c.accent else c.inkDim, ctr,
        )
    }
    // segnalino fisso
    triangle(
        Offset(ctr.x, ctr.y - r * 0.92f),
        Offset(ctr.x - r * 0.035f, ctr.y - r * 1.0f),
        Offset(ctr.x + r * 0.035f, ctr.y - r * 1.0f),
        c.accent,
    )
    textCentered(e.cache.get("${degInt(h)}°", r * 0.30f, FontWeight.Thin), ctr.x, ctr.y - r * 0.04f, c.ink)
    textCentered(e.cache.get(fieldText(e.data.field()), r * 0.07f), ctr.x, ctr.y + r * 0.19f, c.inkDim)
    accuracyBars(e.data.accuracy(), Offset(ctr.x, ctr.y + r * 0.33f), r * 0.07f, c.inkDim)
}

// ---------------------------------------------------------------------------
// 10. Aereo: scala numerata come l'indicatore di rotta degli aerei
// ---------------------------------------------------------------------------
fun DrawScope.drawAereo(e: DialEnv) {
    val c = e.c
    val h = e.data.heading()
    val ctr = center
    val r = min(size.width, size.height) / 2f

    drawCircle(c.ink, r * 0.94f, ctr, style = Stroke(r * 0.012f))
    for (a in 0 until 360 step 5) {
        val big = a % 10 == 0
        val len = if (big) r * 0.07f else r * 0.04f
        tick(ctr, a - h, r * 0.94f - len, r * 0.94f, c.ink, if (big) r * 0.012f else r * 0.007f)
    }
    for (a in 0 until 360 step 30) {
        val t = when (a) {
            0 -> "N"
            90 -> "E"
            180 -> "S"
            270 -> "O"
            else -> (a / 10).toString()
        }
        val card = a % 90 == 0
        radialText(
            e.cache.get(t, r * if (card) 0.13f else 0.11f, if (card) FontWeight.Bold else FontWeight.Medium),
            a - h, r * 0.74f,
            if (a == 0) c.accent else c.ink, ctr,
        )
    }

    // indicatore di rotta in alto
    triangle(
        Offset(ctr.x, ctr.y - r * 0.87f),
        Offset(ctr.x - r * 0.05f, ctr.y - r * 1.0f),
        Offset(ctr.x + r * 0.05f, ctr.y - r * 1.0f),
        c.accent,
    )
    // sagoma dell'aereo
    val w = r * 0.035f
    drawLine(c.accent, Offset(ctr.x, ctr.y - r * 0.26f), Offset(ctr.x, ctr.y + r * 0.26f), w, StrokeCap.Round)
    drawLine(c.accent, Offset(ctr.x - r * 0.24f, ctr.y + r * 0.02f), Offset(ctr.x + r * 0.24f, ctr.y + r * 0.02f), w, StrokeCap.Round)
    drawLine(c.accent, Offset(ctr.x - r * 0.09f, ctr.y + r * 0.23f), Offset(ctr.x + r * 0.09f, ctr.y + r * 0.23f), w, StrokeCap.Round)

    // finestra con i gradi
    val bw = r * 0.46f
    val bh = r * 0.19f
    drawRoundRect(
        c.card, Offset(ctr.x - bw / 2f, ctr.y - r * 0.50f - bh / 2f), Size(bw, bh), CornerRadius(r * 0.04f),
    )
    drawRoundRect(
        c.ink.copy(alpha = 0.6f), Offset(ctr.x - bw / 2f, ctr.y - r * 0.50f - bh / 2f), Size(bw, bh),
        CornerRadius(r * 0.04f), style = Stroke(r * 0.008f),
    )
    textCentered(e.cache.get("${degInt(h)}°", r * 0.13f, FontWeight.Medium), ctr.x, ctr.y - r * 0.50f, c.ink)
    accuracyBars(e.data.accuracy(), Offset(ctr.x, ctr.y + r * 0.52f), r * 0.07f, c.ink)
}

// ---------------------------------------------------------------------------
// 11. Marino: rosa dei venti con stella a otto punte
// ---------------------------------------------------------------------------
private fun DrawScope.rosePoint(ctr: Offset, ang: Float, len: Float, half: Float, color: androidx.compose.ui.graphics.Color) {
    val tip = pol(ctr, len, ang)
    val s1 = pol(ctr, half, ang + 90f)
    val s2 = pol(ctr, half, ang - 90f)
    triangle(tip, s1, ctr, color)
    triangle(tip, s2, ctr, color.copy(alpha = color.alpha * 0.45f))
}

fun DrawScope.drawMarino(e: DialEnv) {
    val c = e.c
    val h = e.data.heading()
    val ctr = center
    val r = min(size.width, size.height) / 2f

    drawCircle(c.ink, r * 0.97f, ctr, style = Stroke(r * 0.008f))
    drawCircle(c.ink.copy(alpha = 0.6f), r * 0.76f, ctr, style = Stroke(r * 0.006f))
    for (a in 0 until 360 step 5) {
        val big = a % 10 == 0
        tick(ctr, a - h, r * 0.97f - if (big) r * 0.045f else r * 0.025f, r * 0.97f, c.ink, if (big) r * 0.01f else r * 0.006f)
    }
    val letters = listOf("N", "E", "S", "O")
    for (i in 0..3) {
        radialText(
            e.cache.get(letters[i], r * 0.11f, FontWeight.Bold),
            i * 90f - h, r * 0.865f,
            if (i == 0) c.accent else c.ink, ctr,
        )
    }

    // stella: prima le punte intermedie, poi quelle cardinali
    for (i in 0..3) {
        rosePoint(ctr, 45f + i * 90f - h, r * 0.50f, r * 0.075f, c.ink.copy(alpha = 0.6f))
    }
    for (i in 0..3) {
        rosePoint(ctr, i * 90f - h, r * 0.72f, r * 0.10f, if (i == 0) c.accent else c.ink)
    }

    triangle(
        Offset(ctr.x, ctr.y - r * 0.90f),
        Offset(ctr.x - r * 0.045f, ctr.y - r * 1.0f),
        Offset(ctr.x + r * 0.045f, ctr.y - r * 1.0f),
        c.accent,
    )
    drawCircle(c.card, r * 0.25f, ctr)
    drawCircle(c.ink, r * 0.25f, ctr, style = Stroke(r * 0.008f))
    textCentered(e.cache.get("${degInt(h)}°", r * 0.16f, FontWeight.Medium), ctr.x, ctr.y - r * 0.03f, c.ink)
    accuracyBars(e.data.accuracy(), Offset(ctr.x, ctr.y + r * 0.12f), r * 0.07f, c.ink)
}

// ---------------------------------------------------------------------------
// 12. Neon: anelli luminosi con alone
// ---------------------------------------------------------------------------
fun DrawScope.drawNeon(e: DialEnv) {
    val c = e.c
    val h = e.data.heading()
    val ctr = center
    val r = min(size.width, size.height) / 2f

    val ring = r * 0.88f
    drawCircle(c.accent.copy(alpha = 0.07f), ring, ctr, style = Stroke(r * 0.12f))
    drawCircle(c.accent.copy(alpha = 0.13f), ring, ctr, style = Stroke(r * 0.07f))
    drawCircle(c.accent.copy(alpha = 0.30f), ring, ctr, style = Stroke(r * 0.035f))
    drawCircle(c.accent, ring, ctr, style = Stroke(r * 0.012f))

    for (a in 0 until 360 step 10) {
        val major = a % 30 == 0
        drawCircle(
            if (major) c.accent else c.ink.copy(alpha = 0.5f),
            if (major) r * 0.016f else r * 0.009f,
            pol(ctr, r * 0.77f, a - h),
        )
    }
    val letters = listOf("N", "E", "S", "O")
    for (i in 0..3) {
        uprightText(
            e.cache.get(letters[i], r * 0.12f, FontWeight.Bold),
            i * 90f - h, r * 0.62f,
            if (i == 0) c.accent else c.ink, ctr,
        )
    }

    // indicatore fisso con alone
    tick(ctr, 0f, r * 0.80f, r * 0.99f, c.accent.copy(alpha = 0.25f), r * 0.06f, StrokeCap.Round)
    tick(ctr, 0f, r * 0.80f, r * 0.99f, c.accent, r * 0.02f, StrokeCap.Round)

    drawCircle(c.accent.copy(alpha = 0.5f), r * 0.38f, ctr, style = Stroke(r * 0.007f))
    textCentered(e.cache.get("${degInt(h)}°", r * 0.24f, FontWeight.Light), ctr.x, ctr.y - r * 0.05f, c.accent)
    textCentered(e.cache.get(fieldText(e.data.field()), r * 0.065f), ctr.x, ctr.y + r * 0.13f, c.inkDim)
    accuracyBars(e.data.accuracy(), Offset(ctr.x, ctr.y + r * 0.24f), r * 0.06f, c.inkDim)
}
