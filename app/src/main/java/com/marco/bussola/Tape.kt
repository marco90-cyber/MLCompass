package com.marco.bussola

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Scala lineare orizzontale: è la vista che compare tenendo il telefono in verticale.
 * Ogni 2° una tacca, ogni 30° una tacca lunga con numero, ai punti cardinali la lettera.
 */
@Composable
fun TapeView(data: DialData, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val tm = rememberTextMeasurer(cacheSize = 16)
    val density = LocalDensity.current
    val cache = remember(tm, density) { TextCache(tm, density) }

    Canvas(modifier) {
        val h = data.heading()
        val w = size.width
        val baseY = size.height * 0.62f
        val cx = w / 2f
        val pxPerDeg = w / 96f

        val hair = 1.dp.toPx()
        drawLine(c.ink.copy(alpha = 0.5f), Offset(0f, baseY), Offset(w, baseY), hair)

        val first = floor(h - 52f).toInt()
        val last = ceil(h + 52f).toInt()
        val letters = mapOf(0 to "N", 90 to "E", 180 to "S", 270 to "O")

        for (a in first..last) {
            if (Math.floorMod(a, 2) != 0) continue
            val n = Math.floorMod(a, 360)
            val x = cx + (a - h) * pxPerDeg
            val major = n % 30 == 0
            val isNorth = n == 0

            val len = if (major) 28.dp.toPx() else 14.dp.toPx()
            val width = if (major) 2.2.dp.toPx() else 1.2.dp.toPx()
            val col: Color = when {
                isNorth -> c.accent
                major -> c.ink
                else -> c.ink.copy(alpha = 0.7f)
            }
            drawLine(col, Offset(x, baseY), Offset(x, baseY - len), width, StrokeCap.Butt)

            if (major) {
                val letter = letters[n]
                if (letter != null) {
                    val l = cache.get(letter, 24.dp.toPx(), FontWeight.Bold)
                    drawText(l, if (isNorth) c.accent else c.ink, Offset(x - l.size.width / 2f, baseY + 8.dp.toPx()))
                } else {
                    val l = cache.get(n.toString(), 16.dp.toPx())
                    drawText(l, c.ink, Offset(x - l.size.width / 2f, baseY + 10.dp.toPx()))
                }
            }
        }

        // direzione bloccata: fascia di scostamento e linea sul riferimento
        val locked = data.locked()
        if (!locked.isNaN()) {
            val d = signedDiff(locked, h)
            val x = cx + d * pxPerDeg
            val x0 = minOf(cx, x).coerceIn(0f, w)
            val x1 = maxOf(cx, x).coerceIn(0f, w)
            drawRect(
                LockRed.copy(alpha = 0.4f),
                Offset(x0, baseY - 9.dp.toPx()),
                Size(x1 - x0, 9.dp.toPx()),
            )
            if (x in 0f..w) {
                drawLine(LockRed, Offset(x, baseY + 6.dp.toPx()), Offset(x, baseY - 44.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
            }
        }

        // freccia fissa al centro
        val tipY = baseY - 64.dp.toPx()
        val headBaseY = tipY + 16.dp.toPx()
        triangle(
            Offset(cx, tipY),
            Offset(cx - 9.dp.toPx(), headBaseY),
            Offset(cx + 9.dp.toPx(), headBaseY),
            c.accent,
        )
        drawLine(c.accent, Offset(cx, headBaseY), Offset(cx, baseY), 3.dp.toPx())
    }
}
