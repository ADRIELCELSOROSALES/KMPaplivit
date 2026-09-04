package com.aplivit.di

import com.aplivit.core.domain.usecase.CompleteGameUseCase
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.domain.usecase.NavigationUseCase
import com.aplivit.core.domain.usecase.SessionResumeUseCase
import com.aplivit.core.domain.usecase.UnlockNextLevelUseCase
import com.aplivit.core.domain.usecase.ValidatePronunciationUseCase
import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.ContentRepository
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechRecognizer
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.core.port.UsageTracker
import com.aplivit.infrastructure.content.LevelsLoader
import com.aplivit.infrastructure.storage.SettingsUsageTracker
import com.aplivit.infrastructure.provideConnectivityChecker
import com.aplivit.infrastructure.provideHttpClientEngine
import com.aplivit.infrastructure.provideSettings
import com.aplivit.infrastructure.provideSpeechRecognizer
import com.aplivit.infrastructure.provideSpeechSynthesizer
import com.aplivit.auth.SessionManager
import com.aplivit.auth.TokenStore
import com.aplivit.auth.providePlatformGameSignIn
import com.aplivit.infrastructure.remote.AttemptApi
import com.aplivit.infrastructure.remote.AuthApi
import com.aplivit.infrastructure.remote.ContentApi
import com.aplivit.infrastructure.remote.MyLanguageApi
import com.aplivit.infrastructure.remote.ProgressApi
import com.aplivit.infrastructure.remote.createApiHttpClient
import com.aplivit.infrastructure.storage.SettingsProgressRepository
import com.aplivit.offline.AccountGuard
import com.aplivit.offline.AttemptQueue
import com.aplivit.offline.BackendExerciseMapper
import com.aplivit.offline.BackendLevelMapper
import com.aplivit.offline.ContentCache
import com.aplivit.offline.OfflineContentRepository
import com.aplivit.offline.SyncCoordinator
import com.aplivit.presentation.screen.exercise.LetterTracingViewModel
import com.aplivit.presentation.screen.exercise.TouchViewModel
import com.aplivit.presentation.screen.settings.SettingsViewModel
import org.koin.dsl.module

val appModule = module {
    single<ConnectivityChecker> { provideConnectivityChecker() }
    single<SpeechSynthesizer> { provideSpeechSynthesizer() }
    single<SpeechRecognizer> { provideSpeechRecognizer(get()) }
    single { provideSettings() }
    single<ProgressRepository> { SettingsProgressRepository(get()) }
    single<UsageTracker> { SettingsUsageTracker(get()) }
    single { LevelsLoader() }

    // --- Auth del alumno (JWT) ---
    single { TokenStore(get()) }
    single { providePlatformGameSignIn() }

    // --- Red + contenido offline-first ---
    single { provideHttpClientEngine() }
    single {
        val tokenStore = get<TokenStore>()
        val scope = this
        createApiHttpClient(
            engine = get(),
            tokenProvider = { tokenStore.token() },
            // SessionManager se resuelve recién acá (y no como dependencia del cliente) para no
            // crear un ciclo: SessionManager -> AuthApi -> HttpClient.
            onUnauthorized = { scope.get<SessionManager>().renewSession() }
        )
    }
    single { AuthApi(get()) }
    single { ContentApi(get()) }
    single { AttemptApi(get()) }
    single { MyLanguageApi(get()) }
    single { ProgressApi(get()) }
    single { ContentCache(get()) }
    single { AttemptQueue(get()) }
    single { SessionManager(get(), get(), get()) }
    single<ContentRepository> {
        OfflineContentRepository(
            contentApi = get(),
            attemptApi = get(),
            cache = get(),
            queue = get(),
            connectivity = get(),
            levelMapper = get(),
            progressRepository = get(),
            myLanguageApi = get(),
            progressApi = get()
        )
    }
    single { BackendExerciseMapper() }
    single { BackendLevelMapper() }
    single { AccountGuard(get(), get(), get(), get(), get()) }
    single { SyncCoordinator(get(), get(), get(), get()) }

    factory { GetLevelsUseCase(get(), get(), get()) }
    factory { CompleteGameUseCase(get()) }
    factory { ValidatePronunciationUseCase() }
    factory { UnlockNextLevelUseCase(get()) }
    factory { NavigationUseCase(get()) }
    factory { SessionResumeUseCase(get()) }
    factory { SettingsViewModel(get(), get(), get()) }
    factory { TouchViewModel(get(), get()) }
    factory { LetterTracingViewModel(get(), get()) }
}
