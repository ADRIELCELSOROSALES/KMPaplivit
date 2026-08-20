package com.aplivit.offline

/**
 * Conversor puro epoch-millis -> ISO-8601 UTC ("2026-08-19T14:30:00.123Z"), idéntico en toda
 * plataforma (no depende de java.time ni NSDateFormatter). Formato que el backend parsea como
 * DateTimeOffset para el `attempted_at` de intentos offline (RF-14). El backend lo trunca a
 * milisegundo, así que emitimos precisión de milisegundo.
 */
object Iso8601 {

    fun fromEpochMillis(epochMillis: Long): String {
        val totalSeconds = epochMillis.floorDiv(1000L)
        val millis = epochMillis.mod(1000L).toInt()

        val days = totalSeconds.floorDiv(86_400L)
        val secondsOfDay = totalSeconds.mod(86_400L).toInt()

        val hour = secondsOfDay / 3600
        val minute = (secondsOfDay % 3600) / 60
        val second = secondsOfDay % 60

        val (year, month, day) = civilFromDays(days)

        return buildString {
            append(pad(year, 4)); append('-')
            append(pad(month, 2)); append('-')
            append(pad(day, 2)); append('T')
            append(pad(hour, 2)); append(':')
            append(pad(minute, 2)); append(':')
            append(pad(second, 2)); append('.')
            append(pad(millis, 3)); append('Z')
        }
    }

    // Algoritmo days-from-civil de Howard Hinnant: día (desde epoch 1970-01-01) -> (año, mes, día).
    private fun civilFromDays(daysSinceEpoch: Long): Triple<Int, Int, Int> {
        val z = daysSinceEpoch + 719_468
        val era = (if (z >= 0) z else z - 146_096) / 146_097
        val doe = z - era * 146_097                                  // [0, 146096]
        val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365  // [0, 399]
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)           // [0, 365]
        val mp = (5 * doy + 2) / 153                                 // [0, 11]
        val d = (doy - (153 * mp + 2) / 5 + 1).toInt()              // [1, 31]
        val m = (if (mp < 10) mp + 3 else mp - 9).toInt()          // [1, 12]
        val year = (if (m <= 2) y + 1 else y).toInt()
        return Triple(year, m, d)
    }

    private fun pad(value: Int, width: Int): String {
        val s = value.toString()
        return if (s.length >= width) s else "0".repeat(width - s.length) + s
    }
}
