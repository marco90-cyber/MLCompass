package com.marco.bussola

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Immutable
data class AppColors(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val card: Color,
    val chipBg: Color,
    val ink: Color,
    val inkDim: Color,
    val outline: Color,
    val accent: Color,
    val onAccent: Color,
)

val LocalAppColors = staticCompositionLocalOf<AppColors> { error("AppColors non fornito") }

fun buildColors(dark: Boolean, accentIn: Color): AppColors {
    // Evita che un colore troppo chiaro (o troppo scuro) sparisca sullo sfondo.
    val accent = when {
        !dark && accentIn.luminance() > 0.75f -> Color(0xFF90A4AE)
        dark && accentIn.luminance() < 0.05f -> Color(0xFF90A4AE)
        else -> accentIn
    }
    val onAccent = if (accent.luminance() > 0.4f) Color(0xFF1B1B1F) else Color.White
    return if (dark) {
        AppColors(
            dark = true,
            bg = Color(0xFF0E121B),
            surface = Color(0xFF262A35),
            card = Color(0xFF1A2130),
            chipBg = Color(0xFF3E4259),
            ink = Color(0xFFE3E2E9),
            inkDim = Color(0xFFA9ABB6),
            outline = Color(0xFF8E909A),
            accent = accent,
            onAccent = onAccent,
        )
    } else {
        AppColors(
            dark = false,
            bg = Color(0xFFF6F7FB),
            surface = Color(0xFFFFFFFF),
            card = Color(0xFFE6EAF3),
            chipBg = Color(0xFFD9DEF2),
            ink = Color(0xFF1B1B21),
            inkDim = Color(0xFF5B5E6B),
            outline = Color(0xFF7A7D8A),
            accent = accent,
            onAccent = onAccent,
        )
    }
}

@Composable
fun BussolaTheme(dark: Boolean, accent: Color, content: @Composable () -> Unit) {
    val colors = remember(dark, accent) { buildColors(dark, accent) }
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.bg,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.bg,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
        )
    }
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
