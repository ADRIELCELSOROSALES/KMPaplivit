package com.aplivit.core.domain.model

data class UsageStats(
    val totalSeconds: Long,
    val sessionsCount: Int,
    val lastSessionDate: String,  // ISO format (yyyy-MM-dd) or "—" if never
    val sessionHistory: List<SessionRecord>
)

data class SessionRecord(
    val date: String,
    val durationSeconds: Long
)
