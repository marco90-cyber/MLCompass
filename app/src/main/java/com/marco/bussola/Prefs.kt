package com.marco.bussola

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Proprietà osservabile da Compose e salvata a ogni modifica. */
class PrefState<T>(initial: T, private val save: (T) -> Unit) : ReadWriteProperty<Any?, T> {
    private val state = mutableStateOf(initial)
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state.value = value
        save(value)
    }
}

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("bussola_prefs", Context.MODE_PRIVATE)

    /** Stile del quadrante scelto. */
    var style by PrefState(DialStyle.fromId(sp.getString("style", null))) {
        sp.edit().putString("style", it.id).apply()
    }

    /** Id del colore principale. */
    var accentId by PrefState(sp.getString("accent", "rosso") ?: "rosso") {
        sp.edit().putString("accent", it).apply()
    }

    /** true = nord geografico (con declinazione), false = nord magnetico. */
    var trueNorth by PrefState(sp.getBoolean("trueNorth", true)) {
        sp.edit().putBoolean("trueNorth", it).apply()
    }

    /** 0 sistema, 1 chiaro, 2 scuro. */
    var theme by PrefState(sp.getInt("theme", 2)) {
        sp.edit().putInt("theme", it).apply()
    }

    /** 0 automatica, 1 sempre quadrante, 2 sempre scala. */
    var viewMode by PrefState(sp.getInt("viewMode", 0)) {
        sp.edit().putInt("viewMode", it).apply()
    }

    /** 0 reattiva, 1 equilibrata, 2 morbida. */
    var smoothing by PrefState(sp.getInt("smoothing", 1)) {
        sp.edit().putInt("smoothing", it).apply()
    }

    var keepScreenOn by PrefState(sp.getBoolean("keepScreenOn", true)) {
        sp.edit().putBoolean("keepScreenOn", it).apply()
    }

    var haptic by PrefState(sp.getBoolean("haptic", false)) {
        sp.edit().putBoolean("haptic", it).apply()
    }

    var showField by PrefState(sp.getBoolean("showField", true)) {
        sp.edit().putBoolean("showField", it).apply()
    }

    var askedLocation by PrefState(sp.getBoolean("askedLocation", false)) {
        sp.edit().putBoolean("askedLocation", it).apply()
    }

    val accent: AccentOption
        get() = AccentOptions.firstOrNull { it.id == accentId } ?: AccentOptions[0]

    // Ultima posizione nota, per calcolare declinazione e campo previsto anche offline.
    fun hasCachedLocation(): Boolean = sp.contains("lat") && sp.contains("lon")
    fun cachedLat(): Double = sp.getFloat("lat", 0f).toDouble()
    fun cachedLon(): Double = sp.getFloat("lon", 0f).toDouble()
    fun cachedAlt(): Double = sp.getFloat("alt", 0f).toDouble()

    fun cacheLocation(lat: Double, lon: Double, alt: Double) {
        sp.edit()
            .putFloat("lat", lat.toFloat())
            .putFloat("lon", lon.toFloat())
            .putFloat("alt", alt.toFloat())
            .apply()
    }
}
