package com.aplivit.infrastructure

import android.content.Context
import com.aplivit.AppContext
import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.SpeechRecognizer
import com.aplivit.core.port.SpeechSynthesizer
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual fun provideSpeechSynthesizer(): SpeechSynthesizer =
    AndroidSpeechSynthesizer(AppContext.context)

actual fun provideSpeechRecognizer(connectivityChecker: ConnectivityChecker): SpeechRecognizer =
    AndroidSpeechRecognizer(AppContext.context, connectivityChecker)

actual fun provideConnectivityChecker(): ConnectivityChecker =
    AndroidConnectivityChecker(AppContext.context)

actual fun provideSettings(): Settings {
    val prefs = AppContext.context.getSharedPreferences("aplivit_prefs", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(prefs)
}
