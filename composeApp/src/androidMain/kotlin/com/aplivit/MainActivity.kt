package com.aplivit

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.core.port.UsageTracker
import com.aplivit.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.getKoin

class MainActivity : ComponentActivity() {

    private var pendingMicPermissionCallback: ((Boolean) -> Unit)? = null

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        pendingMicPermissionCallback?.invoke(isGranted)
        pendingMicPermissionCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AppContext.context = applicationContext
        AppContext.requestMicPermission = { onResult ->
            pendingMicPermissionCallback = onResult
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        startKoin {
            androidContext(this@MainActivity)
            modules(appModule)
        }
        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        getKoin().get<UsageTracker>().startSession()
    }

    override fun onPause() {
        super.onPause()
        getKoin().get<UsageTracker>().endSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            getKoin().get<SpeechSynthesizer>().release()
        }
    }
}
