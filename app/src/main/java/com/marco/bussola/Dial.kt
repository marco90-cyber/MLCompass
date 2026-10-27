package com.marco.bussola

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Quadrante circolare nello stile scelto.
 * Occupa lo spazio che gli viene dato e disegna il cerchio al massimo possibile.
 */
@Composable
fun CompassDial(style: DialStyle, data: DialData, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val tm = rememberTextMeasurer(cacheSize = 16)
    val density = LocalDensity.current
    val cache = remember(tm, density) { TextCache(tm, density) }
    val env = remember(cache, c, data) { DialEnv(cache, c, data) }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (style.hasMeasure) {
            ReadoutHeader(data)
        }
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val side = if (maxWidth < maxHeight) maxWidth else maxHeight
            Canvas(Modifier.size(side)) {
                when (style) {
                    DialStyle.CLASSICO -> drawClassico(env)
                    DialStyle.DIFETTO -> drawDifetto(env)
                    DialStyle.IPHONE -> drawIphone(env)
                    DialStyle.NAVIGATORE -> drawNavigatore(env)
                    DialStyle.GRADUATO -> drawGraduato(env)
                    DialStyle.DESIGN -> drawDesign(env)
                    DialStyle.LEGGIBILE -> drawLeggibile(env)
                }
            }
        }
    }
}

/** Lettura sopra il quadrante: segnale e campo a sinistra, gradi al centro, delta a destra. */
@Composable
private fun ReadoutHeader(data: DialData) {
    val c = LocalAppColors.current
    val field = data.field()
    val expected = data.expected()
    val delta = if (field.isNaN() || expected.isNaN()) null else (field - expected).roundToInt()
    val deltaText = when {
        delta == null -> "–"
        delta > 0 -> "+$delta"
        else -> "$delta"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            SignalBars(data.accuracy(), 20.dp)
            Text(fieldText(field), fontSize = 15.sp, color = c.ink)
        }
        Text(
            "${data.headingInt()}°",
            fontSize = 60.sp,
            fontWeight = FontWeight.Medium,
            color = c.ink,
        )
        Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(deltaText, fontSize = 16.sp, color = c.ink)
            Text("Δ", fontSize = 22.sp, color = c.accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SignalBars(accuracy: Int, height: Dp, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Canvas(modifier.size(width = height * 1.1f, height = height)) {
        accuracyBars(accuracy, Offset(size.width / 2f, size.height / 2f), size.height, c.ink)
    }
}
