package com.aplivit

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aplivit.auth.SessionManager
import com.aplivit.auth.devAuthToken
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.offline.SyncCoordinator
import com.aplivit.presentation.navigation.AppNavigation
import com.aplivit.shared.stringsFor
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.koinInject

@Composable
fun App() {
    var showSplash by remember { mutableStateOf(true) }
    var syncFinished by remember { mutableStateOf(false) }
    val progressRepository: ProgressRepository = koinInject()
    val tts: SpeechSynthesizer = koinInject()
    val session: SessionManager = koinInject()
    val syncCoordinator: SyncCoordinator = koinInject()

    // Login al abrir + sync offline-first. Primero el login nativo real (Play Games en Android /
    // Game Center en iOS); si falla o el usuario cancela, cae al token de dev (solo en debug, ver
    // [devAuthToken]). El sync sube intentos pendientes, refresca el catálogo y reconcilia el
    // progreso de la cuenta. Nada acá bloquea ni rompe la UI (runCatching).
    LaunchedEffect(Unit) {
        if (!session.isSignedIn()) {
            val signedIn = runCatching { session.signIn() }.getOrDefault(false)
            if (!signedIn) devAuthToken()?.let { session.useTokenForDev(it) }
        }
        // El home se muestra recién cuando el sync trajo el progreso de la cuenta, así el alumno
        // arranca en el nivel donde quedó (y no en el que tenía el espejo local). Con tope de
        // tiempo: si la red está lenta, se juega con lo cacheado en vez de dejarlo en el splash.
        runCatching { withTimeoutOrNull(SYNC_TIMEOUT_MS) { syncCoordinator.sync() } }
        syncFinished = true
    }

    MaterialTheme {
        if (showSplash || !syncFinished) {
            val language = progressRepository.getSelectedLanguage()
            val strings = stringsFor(language)
            SplashScreen(
                message = strings.splashWelcome,
                language = language,
                tts = tts
            ) { showSplash = false }
        } else {
            AppNavigation()
        }
    }
}

/** Tope de espera del sync inicial antes de dejar entrar al home con lo que haya cacheado. */
private const val SYNC_TIMEOUT_MS = 8_000L
