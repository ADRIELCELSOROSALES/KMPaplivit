package com.aplivit.infrastructure

import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.SpeechRecognizer
import com.aplivit.core.port.SpeechSynthesizer
import com.russhwolf.settings.Settings

expect fun provideSpeechSynthesizer(): SpeechSynthesizer
expect fun provideSpeechRecognizer(connectivityChecker: ConnectivityChecker): SpeechRecognizer
expect fun provideConnectivityChecker(): ConnectivityChecker
expect fun provideSettings(): Settings
