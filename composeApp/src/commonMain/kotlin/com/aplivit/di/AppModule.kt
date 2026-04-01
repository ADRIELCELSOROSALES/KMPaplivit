package com.aplivit.di

import com.aplivit.core.domain.usecase.CompleteGameUseCase
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.domain.usecase.UnlockNextLevelUseCase
import com.aplivit.core.domain.usecase.ValidatePronunciationUseCase
import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechRecognizer
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.infrastructure.content.LevelsLoader
import com.aplivit.infrastructure.provideConnectivityChecker
import com.aplivit.infrastructure.provideSettings
import com.aplivit.infrastructure.provideSpeechRecognizer
import com.aplivit.infrastructure.provideSpeechSynthesizer
import com.aplivit.infrastructure.storage.SettingsProgressRepository
import com.aplivit.presentation.screen.exercise.TouchViewModel
import com.aplivit.presentation.screen.settings.SettingsViewModel
import org.koin.dsl.module

val appModule = module {
    single<ConnectivityChecker> { provideConnectivityChecker() }
    single<SpeechSynthesizer> { provideSpeechSynthesizer() }
    single<SpeechRecognizer> { provideSpeechRecognizer(get()) }
    single { provideSettings() }
    single<ProgressRepository> { SettingsProgressRepository(get()) }
    single { LevelsLoader() }

    factory { GetLevelsUseCase(get()) }
    factory { CompleteGameUseCase(get()) }
    factory { ValidatePronunciationUseCase() }
    factory { UnlockNextLevelUseCase(get()) }
    factory { SettingsViewModel(get(), get()) }
    factory { TouchViewModel(get(), get()) }
}
