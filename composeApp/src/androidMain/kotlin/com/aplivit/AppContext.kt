package com.aplivit

import android.content.Context

object AppContext {
    lateinit var context: Context
    var requestMicPermission: ((onResult: (Boolean) -> Unit) -> Unit)? = null
}
