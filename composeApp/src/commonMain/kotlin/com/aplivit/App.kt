package com.aplivit

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aplivit.auth.DevAuth
import com.aplivit.auth.SessionManager
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.offline.SyncCoordinator
import com.aplivit.presentation.navigation.AppNavigation
import com.aplivit.shared.stringsFor
import org.koin.compose.koinInject

@Composable
fun App() {
    var showSplash by remember { mutableStateOf(true) }
    val progressRepository: ProgressRepository = koinInject()
    val tts: SpeechSynthesizer = koinInject()
    val session: SessionManager = koinInject()
    val syncCoordinator: SyncCoordinator = koinInject()

    // Login al abrir + sync offline-first. Primero el login nativo real (Play Games en Android /
    // Game Center en iOS); si falla o el usuario cancela, cae al token de dev (fallback para iOS,
    // aún sin Game Center, o si PGS todavía no está del todo configurado). El sync sube intentos
    // pendientes y refresca el catálogo. Nada acá bloquea ni rompe la UI (runCatching).
    LaunchedEffect(Unit) {
        if (!session.isSignedIn()) {
            val signedIn = runCatching { session.signIn() }.getOrDefault(false)
            if (!signedIn) DevAuth.AUTO_LOGIN_TOKEN?.let { session.useTokenForDev(it) }
        }
        runCatching { syncCoordinator.sync() }
    }

    MaterialTheme {
        if (showSplash) {
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
