package com.marco.bussola

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Dati finti che si muovono da soli, per l'anteprima dal vivo. */
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

/** Dati fermi per le miniature degli stili. */
@Composable
private fun rememberStaticData(): DialData = remember {
    DialData(
        heading = { 37f },
        headingInt = { 37 },
        field = { 38f },
        expected = { 45f },
        accuracy = { 3 },
        levelX = { 0.08f },
        levelY = { -0.06f },
    )
}

/**
 * Schermata "Aspetto": anteprima grande che si muove, miniature dei quadranti, colori e sfondo.
 * Ogni scelta si applica subito: non serve confermare.
 */
@Composable
fun PersonalizeScreen(prefs: Prefs, onBack: () -> Unit) {
    val c = LocalAppColors.current
    val demo = rememberDemoData()
    val staticData = rememberStaticData()
    val styles = DialStyle.entries
    val tilesState = rememberLazyListState(
        initialFirstVisibleItemIndex = (styles.indexOf(prefs.style) - 1).coerceAtLeast(0),
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButtonBox(onClick = onBack) { BackIcon(c.ink) }
            Spacer(Modifier.width(8.dp))
            Text("Aspetto", fontSize = 28.sp, fontWeight = FontWeight.Light, color = c.ink)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
        ) {
            // Anteprima dal vivo
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(c.card)
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CompassDial(prefs.style, demo, Modifier.fillMaxWidth().height(330.dp))
                Spacer(Modifier.height(8.dp))
                Text(prefs.style.title, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = c.ink)
                Text(
                    prefs.style.description,
                    fontSize = 14.sp,
                    color = c.inkDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            // Quadranti
            SectionTitle("Quadrante", prefs.style.title)
            LazyRow(
                state = tilesState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(styles) { s ->
                    StyleTile(s, selected = s == prefs.style, data = staticData) { prefs.style = s }
                }
            }

            // Colori
            SectionTitle("Colore", prefs.accent.name)
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for (rowItems in AccentOptions.chunked(6)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (opt in rowItems) {
                            Swatch(opt, opt.id == prefs.accent.id, Modifier.weight(1f)) {
                                prefs.accentId = opt.id
                            }
                        }
                        repeat(6 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            // Sfondo
            SectionTitle("Sfondo", null)
            Box(Modifier.padding(horizontal = 16.dp)) {
                Segmented(listOf("Sistema", "Chiaro", "Scuro"), prefs.theme) { prefs.theme = it }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, value: String?) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title.uppercase(),
            fontSize = 13.sp,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.inkDim,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.accent)
        }
    }
}

@Composable
private fun StyleTile(style: DialStyle, selected: Boolean, data: DialData, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .width(104.dp)
            .clip(shape)
            .background(c.card)
            .border(
                if (selected) 2.5.dp else 1.dp,
                if (selected) c.accent else c.outline.copy(alpha = 0.35f),
                shape,
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompassDial(style, data, Modifier.size(88.dp), compact = true)
        Spacer(Modifier.height(6.dp))
        Text(
            style.title,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) c.accent else c.ink,
            maxLines = 1,
        )
    }
}

@Composable
private fun Swatch(opt: AccentOption, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(16.dp)
    val onColor = if (opt.color.luminance() > 0.5f) Color(0xFF1B1B1F) else Color.White
    Box(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(opt.color)
            .border(
                if (selected) 3.dp else 1.dp,
                if (selected) c.ink else c.ink.copy(alpha = 0.25f),
                shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) CheckIcon(onColor, 26.dp)
    }
}
