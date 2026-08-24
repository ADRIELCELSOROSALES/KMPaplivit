package com.aplivit.infrastructure

/** Returns current time as epoch seconds. */
expect fun nowEpochSeconds(): Long

/** Returns current time as epoch milliseconds (para attempted_at de intentos offline). */
expect fun nowEpochMillis(): Long

/** Returns today's date as "yyyy-MM-dd". */
expect fun todayIsoDate(): String
