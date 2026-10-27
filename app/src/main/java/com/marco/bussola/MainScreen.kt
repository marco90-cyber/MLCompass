package com.marco.bussola

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val WarnYellow = Color(0xFFFFD54F)
private val WarnRed = Color(0xFFFF8A80)

@Composable
fun IconButtonBox(
    highlighted: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val c = LocalAppColors.current
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (highlighted) c.chipBg else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun MainScreen(ctl: CompassController, prefs: Prefs, onSettings: () -> Unit, onAppearance: () -> Unit) {
    val c = LocalAppColors.current
    val ctx = LocalContext.current
    var showSensor by remember { mutableStateOf(false) }
    val tape = when (prefs.viewMode) {
        1 -> false
        2 -> true
        else -> ctl.upright
    }
    val data = remember(ctl) { ctl.dialData() }

    val accColor = when (accuracyLevel(ctl.accuracy)) {
        3 -> c.ink
        2 -> WarnYellow
        else -> WarnRed
    }
    val accFraction = when (accuracyLevel(ctl.accuracy)) {
        3 -> 1f
        2 -> 0.66f
        1 -> 0.4f
        else -> 0.18f
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Barra superiore
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Bussola",
                fontSize = 26.sp,
                color = c.ink,
                modifier = Modifier.weight(1f),
            )
            IconButtonBox(
                highlighted = prefs.viewMode != 0,
                onClick = {
                    if (prefs.viewMode != 0) {
                        prefs.viewMode = 0
                        Toast.makeText(ctx, "Vista automatica", Toast.LENGTH_SHORT).show()
                    } else {
                        prefs.viewMode = if (tape) 2 else 1
                        Toast.makeText(
                            ctx,
                            if (tape) "Vista bloccata: scala" else "Vista bloccata: quadrante",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
            ) { ViewLockIcon(c.ink) }
            IconButtonBox(onClick = { showSensor = true }) {
                SignalIcon(accColor, accFraction)
            }
            IconButtonBox(onClick = onAppearance) { PaletteIcon(c.ink, 26.dp) }
            IconButtonBox(onClick = onSettings) { GearIcon(c.ink) }
        }

        // Campo magnetico in alto a destra
        if (prefs.showField) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(end = 22.dp, top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FieldIcon(c.inkDim)
                Spacer(Modifier.width(8.dp))
                Text(fieldText(ctl.fieldUt), fontSize = 22.sp, color = c.ink)
            }
        }

        // Quadrante o scala, a seconda di come si tiene il telefono
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { ctl.toggleLock() },
                ),
        ) {
            Crossfade(targetState = tape, animationSpec = tween(250), label = "vista") { isTape ->
                if (isTape) {
                    TapePage(ctl, data)
                } else {
                    DialPage(ctl, prefs, data)
                }
            }
        }

        if (!ctl.lockedBearing.isNaN()) {
            LockBadge(ctl)
        }

        // Nord geografico / magnetico
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NorthChip(selected = prefs.trueNorth) { prefs.trueNorth = !prefs.trueNorth }
            if (prefs.trueNorth && ctl.declination.isNaN()) {
                Text(
                    "  posizione non disponibile: uso il nord magnetico",
                    fontSize = 12.sp,
                    color = c.inkDim,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showSensor) {
        SensorDialog(ctl) { showSensor = false }
    }
}

@Composable
private fun DialPage(ctl: CompassController, prefs: Prefs, data: DialData) {
    val c = LocalAppColors.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompassDial(prefs.style, data, Modifier.weight(1f).fillMaxWidth())
        if (!prefs.style.showsName) {
            Text(
                CARDINALS_16[(ctl.headingInt / 22.5f).roundToInt() % 16],
                fontSize = 24.sp,
                color = c.ink,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )
        }
    }
}

@Composable
private fun TapePage(ctl: CompassController, data: DialData) {
    val c = LocalAppColors.current
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TapeView(data, Modifier.fillMaxWidth().height(170.dp))
        Spacer(Modifier.height(14.dp))
        Text("${ctl.headingInt}°", fontSize = 84.sp, fontWeight = FontWeight.Light, color = c.ink)
        Text(
            CARDINALS_16[(ctl.headingInt / 22.5f).roundToInt() % 16],
            fontSize = 30.sp,
            color = c.ink,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NorthChip(selected: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .height(48.dp)
            .clip(shape)
            .background(if (selected) c.chipBg else Color.Transparent)
            .border(1.dp, if (selected) Color.Transparent else c.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            CheckIcon(c.ink)
            Spacer(Modifier.width(8.dp))
        }
        Text("Nord geografico", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = c.ink)
    }
}

// ---------------------------------------------------------------------------
// Dialogo "Stato del sensore"
// ---------------------------------------------------------------------------

@Composable
fun SensorDialog(ctl: CompassController, onDismiss: () -> Unit) {
    val c = LocalAppColors.current
    val level = accuracyLevel(ctl.accuracy)
    val accColor = when (level) {
        3 -> c.ink
        2 -> WarnYellow
        else -> WarnRed
    }
    val fraction = when (level) {
        3 -> 1f
        2 -> 0.66f
        1 -> 0.4f
        else -> 0.18f
    }
    val expected = if (ctl.expectedUt.isNaN()) "non disponibile" else "${ctl.expectedUt.roundToInt()} µT"
    val measured = if (ctl.fieldUt.isNaN()) "–" else "${ctl.fieldUt.roundToInt()} µT"

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = c.surface,
        titleContentColor = c.ink,
        textContentColor = c.inkDim,
        title = { Text("Stato del sensore", fontSize = 28.sp) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SignalIcon(accColor, fraction, 30.dp)
                    Spacer(Modifier.width(14.dp))
                    Text(accuracyLabel(ctl.accuracy), fontSize = 18.sp, color = c.ink)
                }
                Spacer(Modifier.height(18.dp))
                Text("Campo magnetico: $measured", fontSize = 17.sp, color = c.ink)
                Text("Previsto qui: $expected", fontSize = 17.sp, color = c.ink)
                if (!ctl.hasSensor) {
                    Spacer(Modifier.height(8.dp))
                    Text("Questo telefono non ha i sensori necessari per la bussola.", fontSize = 15.sp, color = WarnRed)
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "Ruota il dispositivo con un movimento a forma di otto per migliorare l'accuratezza del sensore.",
                    fontSize = 17.sp,
                    color = c.ink,
                )
                Spacer(Modifier.height(16.dp))
                FigureEight(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 200.dp, height = 110.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = c.accent)
            }
        },
    )
}

/** Otto (simbolo dell'infinito) con un punto che lo percorre: mostra il gesto da fare. */
@Composable
private fun FigureEight(modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val t = rememberInfiniteTransition(label = "otto")
    val phase = t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "fase",
    )
    val strokeColor = if (c.dark) Color(0xFFC9C4D0) else Color(0xFF79747E)

    Canvas(modifier) {
        val a = size.width * 0.40f
        val cx = size.width / 2f
        val cy = size.height / 2f
        fun pt(tt: Float): Offset {
            val s = sin(tt)
            val d = 1f + s * s
            return Offset(cx + a * cos(tt) / d, cy + 1.45f * a * s * cos(tt) / d)
        }
        val path = Path()
        val steps = 160
        for (i in 0..steps) {
            val p = pt(i / steps.toFloat() * 2f * PI.toFloat())
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        drawPath(
            path,
            strokeColor,
            style = Stroke(16.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawCircle(c.accent, 9.dp.toPx(), pt(phase.value * 2f * PI.toFloat()))
    }
}

/** Mostra la direzione bloccata e di quanto ci si è spostati; un tocco la rimuove. */
@Composable
private fun LockBadge(ctl: CompassController) {
    val shape = RoundedCornerShape(50)
    val locked = ctl.lockedBearing.roundToInt()
    val dev = ((ctl.headingInt - locked + 540) % 360) - 180
    val devText = if (dev > 0) "+$dev°" else "$dev°"
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .clip(shape)
                .background(LockRed.copy(alpha = 0.16f))
                .border(1.dp, LockRed, shape)
                .clickable { ctl.toggleLock() }
                .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val c = LocalAppColors.current
            Text("Bloccata a $locked°", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.ink)
            Spacer(Modifier.width(10.dp))
            Text("scostamento $devText", fontSize = 15.sp, color = c.inkDim)
            Spacer(Modifier.width(8.dp))
            CloseIcon(LockRed, 18.dp)
        }
    }
}
