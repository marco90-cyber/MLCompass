package com.marco.bussola

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun SettingsScreen(
    prefs: Prefs,
    ctl: CompassController,
    onBack: () -> Unit,
    onPersonalize: () -> Unit,
    onRequestLocation: () -> Unit,
) {
    val c = LocalAppColors.current
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
            Text("Impostazioni", fontSize = 24.sp, color = c.ink)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            // Personalizzazione
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.card)
                    .clickable(onClick = onPersonalize)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(c.accent),
                    contentAlignment = Alignment.Center,
                ) { PaletteIcon(c.onAccent, 24.dp) }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Personalizzazione", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = c.ink)
                    Text(
                        "Quadrante ${prefs.style.title} · colore ${prefs.accent.name}",
                        fontSize = 14.sp,
                        color = c.inkDim,
                    )
                }
                ChevronIcon(c.inkDim)
            }

            Block(
                "Nord di riferimento",
                declinationText(ctl),
            ) {
                Segmented(listOf("Geografico", "Magnetico"), if (prefs.trueNorth) 0 else 1) {
                    prefs.trueNorth = it == 0
                }
            }

            Block("Tema") {
                Segmented(listOf("Sistema", "Chiaro", "Scuro"), prefs.theme) { prefs.theme = it }
            }

            Block(
                "Vista",
                "In automatico, con il telefono in verticale la bussola passa alla scala.",
            ) {
                Segmented(listOf("Automatica", "Quadrante", "Scala"), prefs.viewMode) { prefs.viewMode = it }
            }

            Block("Movimento", "Più morbido = lancetta più stabile ma un po' più lenta.") {
                Segmented(listOf("Reattivo", "Equilibrato", "Morbido"), prefs.smoothing) { prefs.smoothing = it }
            }

            SwitchRow("Schermo sempre acceso", "Evita lo spegnimento mentre usi la bussola", prefs.keepScreenOn) {
                prefs.keepScreenOn = it
            }
            SwitchRow("Vibrazione verso il Nord", "Un piccolo impulso quando punti al nord", prefs.haptic) {
                prefs.haptic = it
            }
            SwitchRow("Mostra campo magnetico", "Valore in µT in alto a destra", prefs.showField) {
                prefs.showField = it
            }

            Block(
                "Posizione",
                if (ctl.locationGranted) {
                    "Usata solo per calcolare declinazione e campo magnetico previsto."
                } else {
                    "Senza posizione la bussola usa il nord magnetico e non conosce il campo previsto."
                },
            ) {
                if (!ctl.locationGranted) {
                    TextButton(onClick = onRequestLocation) {
                        Text("Concedi il permesso", color = c.accent, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Text("Permesso concesso", fontSize = 15.sp, color = c.ink)
                }
            }

            Text(
                "Bussola 1.0",
                fontSize = 13.sp,
                color = c.inkDim,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
        }
    }
}

private fun declinationText(ctl: CompassController): String {
    val d = ctl.declination
    return if (d.isNaN()) {
        "Declinazione non disponibile: serve la posizione."
    } else {
        "Declinazione magnetica qui: " + String.format(Locale.ITALY, "%+.1f°", d)
    }
}

@Composable
private fun Block(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    val c = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = c.ink)
        if (subtitle != null) Text(subtitle, fontSize = 14.sp, color = c.inkDim)
        content()
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, c.outline, shape),
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .background(if (sel) c.accent else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal,
                    color = if (sel) c.onAccent else c.ink,
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = c.ink)
            Text(subtitle, fontSize = 14.sp, color = c.inkDim)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent,
                checkedTrackColor = c.accent,
                uncheckedThumbColor = c.outline,
                uncheckedTrackColor = c.bg,
                uncheckedBorderColor = c.outline,
            ),
        )
    }
}
