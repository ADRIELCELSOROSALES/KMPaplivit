package com.aplivit.config

import platform.Foundation.NSBundle

/** Lo decide el build setting APLIVIT_OFFLINE_ONLY, expuesto en el Info.plist. */
actual val offlineOnly: Boolean =
    (NSBundle.mainBundle.objectForInfoDictionaryKey(INFO_PLIST_KEY) as? String) == "YES"

private const val INFO_PLIST_KEY = "AplivitOfflineOnly"
