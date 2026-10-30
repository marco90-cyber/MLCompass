package com.marco.bussola

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val LATEST_URL = "https://api.github.com/repos/marco90-cyber/MLCompass/releases/latest"
private const val RELEASES_PAGE = "https://github.com/marco90-cyber/MLCompass/releases/latest"

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val version: String, val url: String) : UpdateState
    data class Failed(val reason: String) : UpdateState
}

/** Versione installata, es. "1.0.17". */
fun installedVersion(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
} catch (_: Exception) {
    "?"
}

/** Confronta due versioni tipo "1.0.17" / "v1.0.18": positivo se a è più nuova di b. */
private fun compareVersions(a: String, b: String): Int {
    fun parts(v: String) = v.trim().removePrefix("v").split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
    val pa = parts(a)
    val pb = parts(b)
    for (i in 0 until maxOf(pa.size, pb.size)) {
        val d = (pa.getOrElse(i) { 0 }) - (pb.getOrElse(i) { 0 })
        if (d != 0) return d
    }
    return 0
}

/** Cerca l'ultima release su GitHub e dice se ce n'è una più nuova di quella installata. */
class UpdateChecker(private val context: Context) {
    var state: UpdateState by mutableStateOf(UpdateState.Idle)
        private set

    private val main = Handler(Looper.getMainLooper())

    fun check() {
        if (state == UpdateState.Checking) return
        state = UpdateState.Checking
        val current = installedVersion(context)
        Thread {
            val result: UpdateState = try {
                val conn = URL(LATEST_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("User-Agent", "MLCompass")
                try {
                    when (val code = conn.responseCode) {
                        200 -> {
                            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                            val tag = json.getString("tag_name")
                            var url = json.optString("html_url", RELEASES_PAGE)
                            val assets = json.optJSONArray("assets")
                            if (assets != null) {
                                for (i in 0 until assets.length()) {
                                    val a = assets.getJSONObject(i)
                                    if (a.optString("name").endsWith(".apk")) {
                                        url = a.getString("browser_download_url")
                                        break
                                    }
                                }
                            }
                            if (compareVersions(tag, current) > 0) {
                                UpdateState.Available(tag.removePrefix("v"), url)
                            } else {
                                UpdateState.UpToDate
                            }
                        }
                        404 -> UpdateState.Failed("Nessuna versione trovata.")
                        else -> UpdateState.Failed("Errore del server ($code).")
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (_: Exception) {
                UpdateState.Failed("Impossibile controllare: verifica la connessione.")
            }
            main.post { state = result }
        }.start()
    }

    fun open(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }
}

/** Voce delle impostazioni: versione installata e aggiornamento in app (Pausa / Riprendi / Annulla). */
@Composable
fun UpdateBlock() {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val version = remember { installedVersion(context) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Aggiornamenti", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = c.ink)
        Text("Versione installata: $version", fontSize = 14.sp, color = c.inkDim)
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = { AppUpdate.open(context) }) {
                Text("Cerca aggiornamenti", color = c.accent, fontWeight = FontWeight.Medium)
            }
        }
        Text(
            "La nuova versione si scarica e si installa direttamente dall'app.",
            fontSize = 14.sp,
            color = c.inkDim,
        )
    }
}
