package com.marco.bussola

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/** Stili di quadrante disponibili nella schermata Personalizzazione. */
enum class DialStyle(
    val id: String,
    val title: String,
    val description: String,
    /** Mostra in alto la lettura completa (campo magnetico, gradi, delta). */
    val hasMeasure: Boolean,
    /** Mostra la bolla di livello. */
    val hasLevel: Boolean,
    /** Il nome della direzione (es. Sud-Sud-Est) è già nel quadrante. */
    val showsName: Boolean,
) {
    CLASSICO(
        "classico", "Classico",
        "Il quadrante originale: scala graduata, lancetta fissa e gradi al centro.",
        hasMeasure = false, hasLevel = false, showsName = true,
    ),
    DIFETTO(
        "difetto", "Difetto",
        "Il quadrante della bussola classico, semplice ed elegante.",
        hasMeasure = false, hasLevel = false, showsName = false,
    ),
    IPHONE(
        "iphone", "iPhone",
        "Un quadrante completo ispirato allo stile classico.",
        hasMeasure = true, hasLevel = true, showsName = false,
    ),
    NAVIGATORE(
        "navigatore", "Navigatore",
        "Un quadrante in stile vintage per navigatori.",
        hasMeasure = true, hasLevel = false, showsName = false,
    ),
    GRADUATO(
        "graduato", "Graduato",
        "Un quadrante di precisione con graduazioni dettagliate.",
        hasMeasure = false, hasLevel = false, showsName = false,
    ),
    DESIGN(
        "design", "Design",
        "Un quadrante dal design moderno e minimalista.",
        hasMeasure = false, hasLevel = false, showsName = false,
    ),
    LEGGIBILE(
        "leggibile", "Leggibile",
        "Quadrante ad alto contrasto per una leggibilità perfetta.",
        hasMeasure = false, hasLevel = false, showsName = false,
    );

    companion object {
        fun fromId(id: String?): DialStyle = entries.firstOrNull { it.id == id } ?: CLASSICO
    }
}

data class AccentOption(val id: String, val name: String, val color: Color)

val AccentOptions = listOf(
    AccentOption("rosso", "Rosso", Color(0xFFD32F2F)),
    AccentOption("blu", "Blu", Color(0xFF1E88E5)),
    AccentOption("verde", "Verde", Color(0xFF43A047)),
    AccentOption("giallo", "Giallo", Color(0xFFFFD600)),
    AccentOption("bianco", "Bianco", Color(0xFFECEFF1)),
    AccentOption("scuro", "Grafite", Color(0xFF607D8B)),
    AccentOption("talpa", "Talpa", Color(0xFFBCAAA4)),
    AccentOption("arancio", "Arancio", Color(0xFFFB8C00)),
    AccentOption("viola", "Viola", Color(0xFF8E24AA)),
    AccentOption("turchese", "Turchese", Color(0xFF00ACC1)),
    AccentOption("rosa", "Rosa", Color(0xFFEC407A)),
)

/**
 * Dati letti dal disegno del quadrante. Sono funzioni, così i valori che cambiano
 * di continuo (come l'angolo) vengono letti solo in fase di disegno e non causano
 * ricomposizioni.
 */
@Stable
class DialData(
    val heading: () -> Float,
    val headingInt: () -> Int,
    val field: () -> Float,
    val expected: () -> Float,
    val accuracy: () -> Int,
    val levelX: () -> Float,
    val levelY: () -> Float,
    /** Direzione bloccata in gradi; NaN se non c'è nessun blocco. */
    val locked: () -> Float = { Float.NaN },
)

enum class Screen { MAIN, SETTINGS, PERSONALIZE }

val CARDINALS_16 = listOf(
    "Nord", "Nord-Nord-Est", "Nord-Est", "Est-Nord-Est",
    "Est", "Est-Sud-Est", "Sud-Est", "Sud-Sud-Est",
    "Sud", "Sud-Sud-Ovest", "Sud-Ovest", "Ovest-Sud-Ovest",
    "Ovest", "Ovest-Nord-Ovest", "Nord-Ovest", "Nord-Nord-Ovest",
)

fun cardinalName(h: Float): String = CARDINALS_16[(h / 22.5f).roundToInt() % 16]

fun degInt(h: Float): Int = ((h.roundToInt() % 360) + 360) % 360

fun fieldText(v: Float): String = if (v.isNaN()) "– µT" else "${v.roundToInt()} µT"

fun accuracyLevel(a: Int): Int = when (a) {
    3 -> 3
    2 -> 2
    1 -> 1
    else -> 0
}

fun accuracyLabel(a: Int): String = when (a) {
    3 -> "Alta accuratezza"
    2 -> "Media accuratezza"
    1 -> "Bassa accuratezza"
    else -> "Accuratezza non affidabile"
}
