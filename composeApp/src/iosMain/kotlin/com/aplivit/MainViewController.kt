package com.aplivit

import androidx.compose.ui.window.ComposeUIViewController
import com.aplivit.di.appModule
import org.koin.core.context.startKoin

private var koinStarted = false

fun MainViewController() = run {
    if (!koinStarted) {
        koinStarted = true
        startKoin {
            modules(appModule)
        }
    }
    ComposeUIViewController {
        App()
    }
}
