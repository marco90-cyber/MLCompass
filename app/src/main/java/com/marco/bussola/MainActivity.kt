package com.marco.bussola

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    private lateinit var prefs: Prefs
    private lateinit var controller: CompassController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = Prefs(applicationContext)
        controller = CompassController(applicationContext, prefs)
        setContent { App(prefs, controller) }
    }

    override fun onResume() {
        super.onResume()
        controller.start()
    }

    override fun onPause() {
        super.onPause()
        controller.stop()
    }
}

@Composable
private fun App(prefs: Prefs, controller: CompassController) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (prefs.theme) {
        1 -> false
        2 -> true
        else -> systemDark
    }

    BussolaTheme(dark = dark, accent = prefs.accent.color) {
        val view = LocalView.current
        val context = LocalContext.current
        var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }
        // Da dove si è aperto "Aspetto": il tasto indietro torna lì
        var appearanceFrom by rememberSaveable { mutableStateOf(Screen.MAIN) }

        // Icone della barra di stato chiare o scure secondo il tema
        SideEffect {
            val window = (view.context as Activity).window
            val controllerCompat = WindowCompat.getInsetsController(window, view)
            controllerCompat.isAppearanceLightStatusBars = !dark
            controllerCompat.isAppearanceLightNavigationBars = !dark
        }

        // Schermo sempre acceso
        DisposableEffect(prefs.keepScreenOn) {
            val window = (context as Activity).window
            if (prefs.keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }

        // Finestra di aggiornamento in app
        AppUpdateHost()

        // Permesso di posizione (serve per nord geografico e campo previsto)
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { controller.startLocation() }
        val askLocation = {
            launcher.launch(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            )
        }
        LaunchedEffect(Unit) {
            if (!controller.hasLocationPermission() && !prefs.askedLocation) {
                prefs.askedLocation = true
                askLocation()
            }
        }

        BackHandler(enabled = screen != Screen.MAIN) {
            screen = if (screen == Screen.PERSONALIZE) appearanceFrom else Screen.MAIN
        }

        Crossfade(targetState = screen, animationSpec = tween(200), label = "schermata") { s ->
            when (s) {
                Screen.MAIN -> MainScreen(
                    ctl = controller,
                    prefs = prefs,
                    onSettings = { screen = Screen.SETTINGS },
                    onAppearance = {
                        appearanceFrom = Screen.MAIN
                        screen = Screen.PERSONALIZE
                    },
                )
                Screen.SETTINGS -> SettingsScreen(
                    prefs = prefs,
                    ctl = controller,
                    onBack = { screen = Screen.MAIN },
                    onPersonalize = {
                        appearanceFrom = Screen.SETTINGS
                        screen = Screen.PERSONALIZE
                    },
                    onRequestLocation = { askLocation() },
                )

                Screen.PERSONALIZE -> PersonalizeScreen(prefs) { screen = appearanceFrom }
            }
        }
    }
}
