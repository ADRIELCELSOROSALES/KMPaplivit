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

    // Auth de dev (único lugar) + sync offline-first al abrir: sube intentos pendientes y refresca
    // el catálogo. No-op sin sesión/red; nunca bloquea ni rompe la UI (runCatching).
    LaunchedEffect(Unit) {
        DevAuth.AUTO_LOGIN_TOKEN?.let { if (!session.isSignedIn()) session.useTokenForDev(it) }
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
