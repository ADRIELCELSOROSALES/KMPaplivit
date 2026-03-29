package com.aplivit

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.aplivit.presentation.navigation.AppNavigation

@Composable
fun App() {
    MaterialTheme {
        AppNavigation()
    }
}
