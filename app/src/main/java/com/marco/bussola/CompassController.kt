package com.marco.bussola

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Legge i sensori e calcola direzione, campo magnetico, livello e declinazione.
 *
 * Telefono in piano: la direzione è quella della parte alta del telefono.
 * Telefono in verticale: la direzione è quella in cui punta il retro (come una fotocamera).
 */
class CompassController(
    private val context: Context,
    private val prefs: Prefs,
) : SensorEventListener, LocationListener {

    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val rotSensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val magSensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val accSensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private companion object {
        /** Gradi di rotazione tra un tick di vibrazione e il successivo. */
        const val HAPTIC_STEP = 5f
    }

    // ---- Stato osservabile ----

    /** Direzione mostrata (geografica o magnetica secondo le impostazioni), 0..360. */
    var heading by mutableFloatStateOf(0f)
        private set

    /** Direzione arrotondata: cambia poco spesso, adatta ai testi. */
    var headingInt by mutableIntStateOf(0)
        private set

    /** Campo magnetico misurato in µT (arrotondato a 0,5). */
    var fieldUt by mutableFloatStateOf(Float.NaN)
        private set

    /** Campo magnetico previsto nel punto in cui ci si trova, in µT. */
    var expectedUt by mutableFloatStateOf(Float.NaN)
        private set

    /** Declinazione magnetica in gradi (positiva a est). */
    var declination by mutableFloatStateOf(Float.NaN)
        private set

    /** Accuratezza del magnetometro: 0 non affidabile ... 3 alta. */
    var accuracy by mutableIntStateOf(SensorManager.SENSOR_STATUS_UNRELIABLE)
        private set

    /** true quando il telefono è tenuto in verticale. */
    var upright by mutableStateOf(false)
        private set

    var levelX by mutableFloatStateOf(0f)
        private set
    var levelY by mutableFloatStateOf(0f)
        private set

    var hasSensor by mutableStateOf(true)
        private set
    var locationGranted by mutableStateOf(false)
        private set

    fun dialData() = DialData(
        heading = { heading },
        headingInt = { headingInt },
        field = { fieldUt },
        expected = { expectedUt },
        accuracy = { accuracy },
        levelX = { levelX },
        levelY = { levelY },
    )

    // ---- Interni ----

    private val rotMat = FloatArray(9)
    private var uprightInternal = false
    private var initialised = false
    private var sinAcc = 0f
    private var cosAcc = 1f
    private var fieldFiltered = Float.NaN
    private var lx = 0f
    private var ly = 0f
    private var hapticAnchor = Float.NaN
    private var lastHapticMs = 0L

    // Alternativa se manca il sensore "rotation vector".
    private val grav = FloatArray(3)
    private val geo = FloatArray(3)
    private var haveGrav = false
    private var haveGeo = false

    fun start() {
        rotSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        magSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        if (rotSensor == null) {
            accSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        }
        hasSensor = rotSensor != null || (magSensor != null && accSensor != null)
        startLocation()
    }

    fun stop() {
        sm.unregisterListener(this)
        try {
            lm.removeUpdates(this)
        } catch (_: SecurityException) {
        }
    }

    // ---- Sensori ----

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val v = if (event.values.size > 4) event.values.copyOf(4) else event.values
                SensorManager.getRotationMatrixFromVector(rotMat, v)
                handleMatrix(rotMat)
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val m = sqrt(x * x + y * y + z * z)
                fieldFiltered = if (fieldFiltered.isNaN()) m else fieldFiltered + 0.15f * (m - fieldFiltered)
                val q = (fieldFiltered * 2f).roundToInt() / 2f
                if (q != fieldUt) fieldUt = q
                if (event.accuracy != accuracy) accuracy = event.accuracy
                if (rotSensor == null) {
                    for (i in 0..2) geo[i] = if (haveGeo) geo[i] + 0.15f * (event.values[i] - geo[i]) else event.values[i]
                    haveGeo = true
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                for (i in 0..2) grav[i] = if (haveGrav) grav[i] + 0.15f * (event.values[i] - grav[i]) else event.values[i]
                haveGrav = true
                if (haveGeo && SensorManager.getRotationMatrix(rotMat, null, grav, geo)) {
                    handleMatrix(rotMat)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD && accuracy != this.accuracy) {
            this.accuracy = accuracy
        }
    }

    private fun smoothingAlpha(): Float = when (prefs.smoothing) {
        0 -> 0.5f
        2 -> 0.08f
        else -> 0.2f
    }

    private fun normalise(d: Float): Float = ((d % 360f) + 360f) % 360f

    private fun handleMatrix(r: FloatArray) {
        // r[8] = componente verticale dell'asse Z del telefono: 1 = piano, 0 = verticale.
        val nz = r[8].coerceIn(-1f, 1f)
        val tilt = Math.toDegrees(acos(abs(nz).toDouble())).toFloat()
        if (uprightInternal) {
            if (tilt < 38f) uprightInternal = false
        } else if (tilt > 55f) {
            uprightInternal = true
        }
        if (uprightInternal != upright) upright = uprightInternal

        val hDeg = if (uprightInternal) {
            // direzione del retro del telefono
            Math.toDegrees(atan2((-r[2]).toDouble(), (-r[5]).toDouble()))
        } else {
            // direzione della parte alta del telefono
            Math.toDegrees(atan2(r[1].toDouble(), r[4].toDouble()))
        }
        val hr = Math.toRadians(hDeg)
        val s = sin(hr).toFloat()
        val c = cos(hr).toFloat()
        if (!initialised) {
            sinAcc = s
            cosAcc = c
            initialised = true
        } else {
            val a = smoothingAlpha()
            sinAcc += a * (s - sinAcc)
            cosAcc += a * (c - cosAcc)
        }
        val magnetic = normalise(Math.toDegrees(atan2(sinAcc.toDouble(), cosAcc.toDouble())).toFloat())
        val decl = declination
        val shown = if (prefs.trueNorth && !decl.isNaN()) magnetic + decl else magnetic
        val h = normalise(shown)
        heading = h
        val hi = degInt(h)
        if (hi != headingInt) headingInt = hi

        // Livello: la bolla va verso il lato più alto.
        lx += 0.2f * (r[6] - lx)
        ly += 0.2f * (r[7] - ly)
        levelX = lx
        levelY = ly

        maybeHaptic(h)
    }

    /**
     * Risposta tattile: un piccolo tick ogni [HAPTIC_STEP] gradi di rotazione e un impulso
     * più deciso quando si attraversa un punto cardinale (N, E, S, O).
     * L'ancora si sposta solo dopo un passo completo, così il sensore che "trema" fermo
     * su un valore non fa vibrare di continuo.
     */
    private fun maybeHaptic(h: Float) {
        if (!prefs.haptic) {
            hapticAnchor = Float.NaN
            return
        }
        if (hapticAnchor.isNaN()) {
            hapticAnchor = h
            return
        }
        var diff = h - hapticAnchor
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        if (abs(diff) < HAPTIC_STEP) return

        val old = hapticAnchor
        hapticAnchor = if (abs(diff) > 3 * HAPTIC_STEP) {
            h // rotazione veloce: riparte dalla posizione attuale
        } else {
            normalise(old + HAPTIC_STEP * (if (diff > 0f) 1f else -1f))
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastHapticMs < 40L) return
        lastHapticMs = now
        val crossedCardinal = floor(old / 90f).toInt() != floor(hapticAnchor / 90f).toInt()
        vibrate(strong = crossedCardinal)
    }

    private fun vibrate(strong: Boolean) {
        @Suppress("DEPRECATION")
        val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vib.hasVibrator()) return
        val effect = if (Build.VERSION.SDK_INT >= 29) {
            VibrationEffect.createPredefined(
                if (strong) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_TICK,
            )
        } else {
            if (strong) VibrationEffect.createOneShot(35, 220) else VibrationEffect.createOneShot(10, 90)
        }
        vib.vibrate(effect)
    }

    // ---- Posizione (per declinazione e campo previsto) ----

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun startLocation() {
        if (prefs.hasCachedLocation()) {
            applyLocation(prefs.cachedLat(), prefs.cachedLon(), prefs.cachedAlt())
        }
        locationGranted = hasLocationPermission()
        if (!locationGranted) return

        var best: Location? = null
        val providers = try {
            lm.getProviders(true)
        } catch (_: SecurityException) {
            emptyList<String>()
        }
        for (p in providers) {
            try {
                val l = lm.getLastKnownLocation(p)
                if (l != null && (best == null || l.time > best.time)) best = l
            } catch (_: SecurityException) {
            } catch (_: IllegalArgumentException) {
            }
        }
        best?.let { onLocationChanged(it) }

        try {
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 300_000L, 1000f, this, Looper.getMainLooper())
            }
            lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 300_000L, 1000f, this, Looper.getMainLooper())
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun applyLocation(lat: Double, lon: Double, alt: Double) {
        val gf = GeomagneticField(lat.toFloat(), lon.toFloat(), alt.toFloat(), System.currentTimeMillis())
        declination = gf.declination
        expectedUt = gf.fieldStrength / 1000f
    }

    override fun onLocationChanged(location: Location) {
        applyLocation(location.latitude, location.longitude, location.altitude)
        prefs.cacheLocation(location.latitude, location.longitude, location.altitude)
    }

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    @Deprecated("Necessario sulle versioni di Android precedenti alla 29")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
