package com.aplivit.config

import com.aplivit.BuildConfig

/**
 * Backend de desarrollo en Android:
 *  - Teléfono físico por USB (o emulador): correr `adb reverse tcp:5050 tcp:5050` una vez, y
 *    `localhost:5050` del dispositivo llega a la Mac.
 *  - Emulador puro sin adb reverse: usar "http://10.0.2.2:5050" (10.0.2.2 = host desde el emulador).
 */
private const val DEV_API_BASE_URL = "http://localhost:5050"

actual val apiBaseUrl: String =
    if (BuildConfig.DEBUG) DEV_API_BASE_URL else PRODUCTION_API_BASE_URL
