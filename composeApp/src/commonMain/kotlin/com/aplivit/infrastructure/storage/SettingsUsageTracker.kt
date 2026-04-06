package com.aplivit.infrastructure.storage

import com.aplivit.core.domain.model.SessionRecord
import com.aplivit.core.domain.model.UsageStats
import com.aplivit.core.port.UsageTracker
import com.aplivit.infrastructure.nowEpochSeconds
import com.aplivit.infrastructure.todayIsoDate
import com.russhwolf.settings.Settings

/**
 * Persists usage time using multiplatform-settings.
 *
 * Session history is stored as a semicolon-separated string:
 *   "yyyy-MM-dd|seconds;yyyy-MM-dd|seconds;..."
 * Capped at [MAX_HISTORY] entries (most recent kept).
 */
class SettingsUsageTracker(private val settings: Settings) : UsageTracker {

    override fun startSession() {
        settings.putLong(KEY_SESSION_START, nowEpochSeconds())
    }

    override fun endSession() {
        val start = settings.getLongOrNull(KEY_SESSION_START) ?: return
        val duration = nowEpochSeconds() - start
        if (duration <= 0) return

        val today = todayIsoDate()
        settings.putLong(KEY_TOTAL_SECONDS, settings.getLong(KEY_TOTAL_SECONDS, 0L) + duration)
        settings.putInt(KEY_SESSIONS_COUNT, settings.getInt(KEY_SESSIONS_COUNT, 0) + 1)
        settings.putString(KEY_LAST_DATE, today)

        val history = loadRawHistory().toMutableList()
        history.add("$today|$duration")
        if (history.size > MAX_HISTORY) history.removeAt(0)
        settings.putString(KEY_HISTORY, history.joinToString(";"))

        settings.remove(KEY_SESSION_START)
    }

    override fun getStats(): UsageStats = UsageStats(
        totalSeconds = settings.getLong(KEY_TOTAL_SECONDS, 0L),
        sessionsCount = settings.getInt(KEY_SESSIONS_COUNT, 0),
        lastSessionDate = settings.getString(KEY_LAST_DATE, "—"),
        sessionHistory = loadRawHistory().mapNotNull(::parseRecord).reversed()
    )

    override fun clearStats() {
        listOf(KEY_TOTAL_SECONDS, KEY_SESSIONS_COUNT, KEY_LAST_DATE, KEY_HISTORY, KEY_SESSION_START)
            .forEach { settings.remove(it) }
    }

    private fun loadRawHistory(): List<String> {
        val raw = settings.getString(KEY_HISTORY, "")
        return if (raw.isBlank()) emptyList() else raw.split(";").filter { it.isNotBlank() }
    }

    private fun parseRecord(raw: String): SessionRecord? {
        val parts = raw.split("|")
        if (parts.size != 2) return null
        return SessionRecord(date = parts[0], durationSeconds = parts[1].toLongOrNull() ?: return null)
    }

    companion object {
        private const val KEY_TOTAL_SECONDS = "usage_total_seconds"
        private const val KEY_SESSIONS_COUNT = "usage_sessions_count"
        private const val KEY_LAST_DATE = "usage_last_date"
        private const val KEY_HISTORY = "usage_session_history"
        private const val KEY_SESSION_START = "usage_session_start"
        private const val MAX_HISTORY = 60
    }
}
