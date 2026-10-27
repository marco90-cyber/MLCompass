package com.marco.bussola

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Tutte le icone sono disegnate a mano: nessuna dipendenza extra.

fun DrawScope.iconGear(color: Color) {
    val s = size.minDimension
    drawCircle(color, s * 0.28f, center, style = Stroke(s * 0.14f))
    for (i in 0 until 8) {
        tick(center, i * 45f, s * 0.34f, s * 0.46f, color, s * 0.14f)
    }
}

/** Triangolo "segnale": si riempie in base alla frazione (0..1). */
fun DrawScope.iconSignal(color: Color, fraction: Float) {
    val w = size.width
    val h = size.height
    val p = Path().apply {
        moveTo(w * 0.92f, h * 0.08f)
        lineTo(w * 0.92f, h * 0.92f)
        lineTo(w * 0.08f, h * 0.92f)
        close()
    }
    drawPath(p, color.copy(alpha = 0.3f))
    clipRect(top = h * (1f - fraction)) {
        drawPath(p, color)
    }
}

/** Telefono inclinato con lucchetto: blocca la vista (quadrante / scala). */
fun DrawScope.iconViewLock(color: Color) {
    val s = size.minDimension
    val pc = Offset(s * 0.40f, s * 0.60f)
    rotate(-40f, pc) {
        drawRoundRect(
            color,
            Offset(pc.x - s * 0.17f, pc.y - s * 0.30f),
            Size(s * 0.34f, s * 0.60f),
            CornerRadius(s * 0.07f),
            style = Stroke(s * 0.075f),
        )
    }
    drawRoundRect(color, Offset(s * 0.62f, s * 0.24f), Size(s * 0.30f, s * 0.22f), CornerRadius(s * 0.04f))
    drawArc(
        color, 180f, 180f, false,
        Offset(s * 0.67f, s * 0.08f), Size(s * 0.20f, s * 0.28f),
        style = Stroke(s * 0.05f),
    )
}

/** ((•)) — onde attorno a un punto: campo magnetico. */
fun DrawScope.iconField(color: Color) {
    val s = size.minDimension
    drawCircle(color, s * 0.09f, center)
    for (rr in listOf(0.26f, 0.42f)) {
        val d = s * rr
        val stroke = Stroke(s * 0.07f, cap = StrokeCap.Round)
        drawArc(color, -45f, 90f, false, Offset(center.x - d, center.y - d), Size(d * 2, d * 2), style = stroke)
        drawArc(color, 135f, 90f, false, Offset(center.x - d, center.y - d), Size(d * 2, d * 2), style = stroke)
    }
}

fun DrawScope.iconCheck(color: Color) {
    val s = size.minDimension
    val p = Path().apply {
        moveTo(s * 0.20f, s * 0.52f)
        lineTo(s * 0.42f, s * 0.72f)
        lineTo(s * 0.80f, s * 0.30f)
    }
    drawPath(p, color, style = Stroke(s * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

fun DrawScope.iconClose(color: Color) {
    val s = size.minDimension
    val w = s * 0.12f
    drawLine(color, Offset(s * 0.25f, s * 0.25f), Offset(s * 0.75f, s * 0.75f), w, StrokeCap.Round)
    drawLine(color, Offset(s * 0.75f, s * 0.25f), Offset(s * 0.25f, s * 0.75f), w, StrokeCap.Round)
}

fun DrawScope.iconBack(color: Color) {
    val s = size.minDimension
    val w = s * 0.09f
    drawLine(color, Offset(s * 0.18f, s * 0.5f), Offset(s * 0.86f, s * 0.5f), w, StrokeCap.Round)
    drawLine(color, Offset(s * 0.18f, s * 0.5f), Offset(s * 0.48f, s * 0.2f), w, StrokeCap.Round)
    drawLine(color, Offset(s * 0.18f, s * 0.5f), Offset(s * 0.48f, s * 0.8f), w, StrokeCap.Round)
}

fun DrawScope.iconChevron(color: Color) {
    val s = size.minDimension
    val w = s * 0.09f
    drawLine(color, Offset(s * 0.38f, s * 0.22f), Offset(s * 0.66f, s * 0.5f), w, StrokeCap.Round)
    drawLine(color, Offset(s * 0.38f, s * 0.78f), Offset(s * 0.66f, s * 0.5f), w, StrokeCap.Round)
}

fun DrawScope.iconPalette(color: Color) {
    val s = size.minDimension
    drawCircle(color, s * 0.40f, center, style = Stroke(s * 0.08f))
    val dots = listOf(
        Offset(0.34f, 0.40f), Offset(0.52f, 0.30f),
        Offset(0.68f, 0.44f), Offset(0.34f, 0.60f),
    )
    for (d in dots) drawCircle(color, s * 0.06f, Offset(s * d.x, s * d.y))
}

// ---- Versioni composable ----

@Composable
fun GearIcon(color: Color, size: Dp = 26.dp) {
    Canvas(Modifier.size(size)) { iconGear(color) }
}

@Composable
fun SignalIcon(color: Color, fraction: Float, size: Dp = 26.dp) {
    Canvas(Modifier.size(size)) { iconSignal(color, fraction) }
}

@Composable
fun ViewLockIcon(color: Color, size: Dp = 26.dp) {
    Canvas(Modifier.size(size)) { iconViewLock(color) }
}

@Composable
fun FieldIcon(color: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) { iconField(color) }
}

@Composable
fun CheckIcon(color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) { iconCheck(color) }
}

@Composable
fun CloseIcon(color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) { iconClose(color) }
}

@Composable
fun BackIcon(color: Color, size: Dp = 26.dp) {
    Canvas(Modifier.size(size)) { iconBack(color) }
}

@Composable
fun ChevronIcon(color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) { iconChevron(color) }
}

@Composable
fun PaletteIcon(color: Color, size: Dp = 28.dp) {
    Canvas(Modifier.size(size)) { iconPalette(color) }
}
