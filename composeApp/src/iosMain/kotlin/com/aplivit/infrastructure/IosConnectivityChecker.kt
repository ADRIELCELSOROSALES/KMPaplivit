package com.aplivit.infrastructure

import com.aplivit.core.port.ConnectivityChecker

class IosConnectivityChecker : ConnectivityChecker {
    override fun isConnected(): Boolean = true
}
