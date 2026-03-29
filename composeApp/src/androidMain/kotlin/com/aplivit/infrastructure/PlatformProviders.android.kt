package com.aplivit.infrastructure

import android.preference.PreferenceManager
import com.aplivit.AppContext
import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.SpeechRecognizer
import com.aplivit.core.port.SpeechSynthesizer
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual fun provideSpeechSynthesizer(): SpeechSynthesizer =
    AndroidSpeechSynthesizer(AppContext.context)

actual fun provideSpeechRecognizer(connectivityChecker: ConnectivityChecker): SpeechRecognizer =
    if (connectivityChecker.isConnected()) AndroidSpeechRecognizer(AppContext.context)
    else AmplitudeSpeechRecognizer(AppContext.context)

actual fun provideConnectivityChecker(): ConnectivityChecker =
    AndroidConnectivityChecker(AppContext.context)

@Suppress("DEPRECATION")
actual fun provideSettings(): Settings =
    SharedPreferencesSettings(PreferenceManager.getDefaultSharedPreferences(AppContext.context))
