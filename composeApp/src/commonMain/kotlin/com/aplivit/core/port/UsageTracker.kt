package com.aplivit.core.port

import com.aplivit.core.domain.model.UsageStats

interface UsageTracker {
    fun startSession()
    fun endSession()
    fun getStats(): UsageStats
    fun clearStats()
}
