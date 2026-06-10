package com.aplivit.infrastructure

/** Returns current time as epoch seconds. */
expect fun nowEpochSeconds(): Long

/** Returns today's date as "yyyy-MM-dd". */
expect fun todayIsoDate(): String
