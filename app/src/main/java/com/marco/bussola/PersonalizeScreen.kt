package com.marco.bussola

import android.widget.Toast
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Dati finti che si muovono da soli, per l'anteprima "Animazione demo". */
@Composable
fun rememberDemoData(): DialData {
    val t = rememberInfiniteTransition(label = "demo")
    val phase: State<Float> = t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "fase",
    )
    return remember(phase) {
        val twoPi = (2.0 * PI).toFloat()
        fun hd(): Float {
            val v = 37f + 38f * sin(phase.value * twoPi)
            return if (v < 0f) v + 360f else v
        }
        DialData(
            heading = { hd() },
            headingInt = { degInt(hd()) },
            field = { 38f + 2f * sin(phase.value * 2f * twoPi) },
            expected = { 45f },
            accuracy = { 3 },
            levelX = { 0.22f * sin(phase.value * 3f * twoPi) },
            levelY = { 0.18f * cos(phase.value * 2f * twoPi) },
        )
    }
}

@Composable
fun PersonalizeScreen(prefs: Prefs, onBack: () -> Unit) {
    val c = LocalAppColors.current
    val ctx = LocalContext.current
    val styles = DialStyle.entries
    val pager = rememberPagerState(initialPage = styles.indexOf(prefs.style)) { styles.size }
    val demo = rememberDemoData()

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        // Intestazione colorata
        Row(
            Modifier
                .fillMaxWidth()
                .background(c.accent)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButtonBox(onClick = onBack) { BackIcon(c.onAccent) }
            Column(Modifier.padding(start = 8.dp)) {
                Text("Personalizzazione", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = c.onAccent)
                Text("Personalizza la tua bussola", fontSize = 15.sp, color = c.onAccent.copy(alpha = 0.9f))
            }
        }

        // Colori
        Text(
            "Colori",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = c.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 14.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(AccentOptions) { opt ->
                ColorDot(opt, selected = opt.id == prefs.accent.id) { prefs.accentId = opt.id }
            }
        }
        Spacer(Modifier.height(18.dp))

        // Carosello degli stili
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 44.dp),
            pageSpacing = 10.dp,
        ) { page ->
            val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
            StyleCard(
                style = styles[page],
                selected = styles[page] == prefs.style,
                demo = demo,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val a = abs(offset).coerceIn(0f, 1f)
                        alpha = 1f - 0.5f * a
                        scaleX = 1f - 0.06f * a
                        scaleY = 1f - 0.06f * a
                        rotationY = offset * 26f
                        cameraDistance = 14f * density
                    },
            )
        }

        // Indicatore di pagina
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in styles.indices) {
                val on = i == pager.currentPage
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .width(if (on) 34.dp else 24.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (on) c.accent else c.ink.copy(alpha = 0.25f)),
                )
            }
        }

        // Pulsante di selezione
        val current = styles[pager.currentPage]
        val inUse = current == prefs.style
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(c.accent)
                .clickable {
                    prefs.style = current
                    Toast.makeText(ctx, "Quadrante ${current.title} applicato", Toast.LENGTH_SHORT).show()
                    onBack()
                }
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PaletteIcon(c.onAccent)
            Text(
                if (inUse) "Quadrante in uso" else "Seleziona questo quadrante",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = c.onAccent,
            )
            Spacer(Modifier.width(28.dp))
        }
    }
}

@Composable
private fun ColorDot(opt: AccentOption, selected: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val onColor = if (opt.color.red * 0.299f + opt.color.green * 0.587f + opt.color.blue * 0.114f > 0.6f) {
        Color(0xFF1B1B1F)
    } else {
        Color.White
    }
    Box(
        Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(opt.color)
            .border(if (selected) 3.dp else 1.5.dp, if (selected) c.ink else c.ink.copy(alpha = 0.4f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) CheckIcon(onColor, 28.dp)
    }
}

@Composable
private fun StyleCard(style: DialStyle, selected: Boolean, demo: DialData, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .clip(shape)
            .background(c.card)
            .border(2.dp, c.accent, shape),
    ) {
        // Titolo
        Box(
            Modifier
                .fillMaxWidth()
                .background(c.accent)
                .padding(vertical = 14.dp),
        ) {
            Text(
                style.title,
                modifier = Modifier.align(Alignment.Center),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = c.onAccent,
            )
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(c.onAccent),
                    contentAlignment = Alignment.Center,
                ) { CheckIcon(c.accent, 22.dp) }
            }
        }
        // Descrizione
        Text(
            style.description,
            fontSize = 17.sp,
            color = c.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )
        // Anteprima animata
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            CompassDial(style, demo, Modifier.fillMaxSize())
        }
        Text(
            "Animazione demo",
            fontSize = 15.sp,
            fontStyle = FontStyle.Italic,
            color = c.ink.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        )
        // Funzioni dello stile
        Row(
            Modifier
                .fillMaxWidth()
                .background(c.accent)
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Feature("Misurazione", style.hasMeasure)
            Feature("Livello", style.hasLevel)
        }
    }
}

@Composable
private fun Feature(label: String, on: Boolean) {
    val c = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (on) CheckIcon(c.onAccent, 22.dp) else CloseIcon(c.onAccent, 22.dp)
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.onAccent)
    }
}
