package com.aplivit

import android.app.Activity
import android.content.Context
import java.lang.ref.WeakReference

object AppContext {
    lateinit var context: Context
    var requestMicPermission: ((onResult: (Boolean) -> Unit) -> Unit)? = null

    // Activity actual (referencia débil para no filtrarla). Play Games v2 exige una Activity
    // para lanzar el sign-in y pedir el server_auth_code; el applicationContext no alcanza.
    private var activityRef: WeakReference<Activity>? = null
    var activity: Activity?
        get() = activityRef?.get()
        set(value) { activityRef = value?.let { WeakReference(it) } }
}
