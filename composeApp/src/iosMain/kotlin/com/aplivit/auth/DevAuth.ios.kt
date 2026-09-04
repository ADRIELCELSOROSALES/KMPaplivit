package com.aplivit.auth

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import platform.Foundation.NSProcessInfo

/**
 * En iOS no hay BuildConfig: el token se pasa como variable de entorno del scheme de Xcode
 * (Run > Arguments > Environment Variables), que no queda en el binario. Solo se lee en un
 * binario debug.
 */
@OptIn(ExperimentalNativeApi::class)
actual fun devAuthToken(): String? {
    if (!Platform.isDebugBinary) return null
    val value = NSProcessInfo.processInfo.environment[ENV_KEY] as? String
    return value?.takeIf { it.isNotBlank() }
}

private const val ENV_KEY = "APLIVIT_DEV_JWT"
