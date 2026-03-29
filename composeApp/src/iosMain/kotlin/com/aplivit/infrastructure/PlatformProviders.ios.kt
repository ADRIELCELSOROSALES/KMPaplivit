package com.aplivit.infrastructure

import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.SpeechRecognizer
import com.aplivit.core.port.SpeechSynthesizer
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual fun provideSpeechSynthesizer(): SpeechSynthesizer = IosSpeechSynthesizer()

actual fun provideSpeechRecognizer(connectivityChecker: ConnectivityChecker): SpeechRecognizer =
    IosSpeechRecognizer(connectivityChecker)

actual fun provideConnectivityChecker(): ConnectivityChecker = IosConnectivityChecker()

actual fun provideSettings(): Settings =
    NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
