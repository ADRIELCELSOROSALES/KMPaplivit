package com.aplivit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.offline.SyncCoordinator
import com.aplivit.presentation.dev.DevOfflinePanel
import com.aplivit.presentation.navigation.AppNavigation
import com.aplivit.shared.stringsFor
import org.koin.compose.koinInject

/** Interruptor del panel de dev offline (BORRAR junto con DevOfflinePanel al terminar de probar). */
private const val SHOW_DEV_OFFLINE_PANEL = true

@Composable
fun App() {
    var showSplash by remember { mutableStateOf(true) }
    val progressRepository: ProgressRepository = koinInject()
    val tts: SpeechSynthesizer = koinInject()

    // Sync offline-first en background al abrir: sube intentos pendientes y refresca el catálogo.
    // No-op sin sesión o sin internet; nunca bloquea ni rompe la UI (por eso el runCatching).
    val syncCoordinator: SyncCoordinator = koinInject()
    LaunchedEffect(Unit) {
        runCatching { syncCoordinator.sync() }
    }

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
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
            if (SHOW_DEV_OFFLINE_PANEL) {
                DevOfflinePanel(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}
