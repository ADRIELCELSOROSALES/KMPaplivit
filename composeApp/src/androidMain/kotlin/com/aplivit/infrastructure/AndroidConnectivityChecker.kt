package com.aplivit.infrastructure

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.aplivit.core.port.ConnectivityChecker

class AndroidConnectivityChecker(private val context: Context) : ConnectivityChecker {
    override fun isConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
