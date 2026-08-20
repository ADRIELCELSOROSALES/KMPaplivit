package com.aplivit.presentation.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplivit.auth.SessionManager
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.ContentRepository
import com.aplivit.offline.BackendExerciseMapper
import com.aplivit.offline.MappedExercise
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Panel de DEV temporal para ejercitar la capa offline en un dispositivo real. Inyecta un token de
 * Student de dev, baja el catálogo y permite registrar/flushear intentos. BORRAR al terminar.
 */
private const val DEV_STUDENT_TOKEN =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI5N2U2YTAzYi03ZDQ1LTQ2OTItOTU1My1hMTFiZWIxZDIyZTIiLCJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL3dzLzIwMDgvMDYvaWRlbnRpdHkvY2xhaW1zL3JvbGUiOiJTdHVkZW50IiwiZW1haWwiOiIiLCJpc3MiOiJBcGxpdml0IiwiYXVkIjoiQXBsaXZpdCIsImlhdCI6MTc4NzIzMzE2OCwibmJmIjoxNzg3MjMzMTY4LCJleHAiOjE3ODk4MjUxNjh9.hjI0pUlJfDwDerKFmnlUmUyrylChwk33aID963X398s"

private fun log(msg: String) = println("OFFLINE_HARNESS: $msg")

@Composable
fun DevOfflinePanel(modifier: Modifier = Modifier) {
    val session: SessionManager = koinInject()
    val content: ContentRepository = koinInject()
    val mapper: BackendExerciseMapper = koinInject()
    val connectivity: ConnectivityChecker = koinInject()
    val getLevels: GetLevelsUseCase = koinInject()
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("iniciando…") }

    LaunchedEffect(Unit) {
        runCatching {
            session.useTokenForDev(DEV_STUDENT_TOKEN)
            // Pequeño delay: al arranque en frío ConnectivityManager puede reportar "sin red"
            // durante los primeros ms del proceso (falso negativo).
            delay(1200)
            log("signed=${session.isSignedIn()} connected=${connectivity.isConnected()}")

            // Path de producción: refreshContent() baja el lote y lo cachea.
            val result = content.refreshContent()
            log("refresh=$result")

            val cached = content.cachedExercises()
            val first = cached.firstOrNull()?.let { mapper.map(it) }
            val firstDesc = when (first) {
                is MappedExercise.Vocalize -> "${first.exercise.type} '${first.exercise.content}'"
                is MappedExercise.Unsupported -> "unsupported"
                null -> "—"
            }
            // Path REAL que consume Home: niveles reconstruidos desde el backend.
            val levels = getLevels.execute()
            log("getLevels: ${levels.size} niveles · 1º='${levels.firstOrNull()?.word}' · último='${levels.lastOrNull()?.word}'")

            status = "cache=${cached.size} ej · niveles=${levels.size} · 1º=$firstDesc"
            log(status)

            // Completar el nivel 1 vía la ruta de producción (la misma que usa GameScreen).
            val done = content.submitLevelCompleted(1)
            log("submitLevelCompleted(1)=$done pend=${content.pendingAttemptCount()}")
        }.onFailure { status = "ERROR: ${it::class.simpleName}: ${it.message}"; log(status) }
    }

    Column(
        modifier = modifier.fillMaxWidth().background(Color(0xEE10131A)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("DEV offline harness", color = Color(0xFF6EE7B7), fontSize = 12.sp)
        Text(status, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    runCatching {
                        val ex = content.cachedExercises().firstOrNull()
                        if (ex == null) { status = "sin ejercicios en cache"; return@launch }
                        val r = content.submitAttempt(ex, givenAnswer = "MA", voiceIsCorrect = true)
                        status = "intento: correct=${r.isCorrect} online=${r.sentOnline} · pend=${content.pendingAttemptCount()}"
                        log(status)
                    }.onFailure { status = "submit ERROR: ${it.message}"; log(status) }
                }
            }) { Text("Submit", fontSize = 12.sp) }

            Button(onClick = {
                scope.launch {
                    runCatching {
                        val f = content.flushPendingAttempts()
                        status = "flush: synced=${f.synced} already=${f.alreadySynced} err=${f.errors} · pend=${content.pendingAttemptCount()}"
                        log(status)
                    }.onFailure { status = "flush ERROR: ${it.message}"; log(status) }
                }
            }) { Text("Flush", fontSize = 12.sp) }
        }
    }
}
