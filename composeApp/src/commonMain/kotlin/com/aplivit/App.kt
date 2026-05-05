package com.aplivit

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aplivit.presentation.navigation.AppNavigation

@Composable
fun App() {
    var showSplash by remember { mutableStateOf(true) }
    MaterialTheme {
        if (showSplash) {
            SplashScreen { showSplash = false }
        } else {
            AppNavigation()
        }
    }
}
