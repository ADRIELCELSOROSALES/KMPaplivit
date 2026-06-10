package com.aplivit.infrastructure

import java.util.Calendar

actual fun nowEpochSeconds(): Long = System.currentTimeMillis() / 1000L

actual fun todayIsoDate(): String {
    val c = Calendar.getInstance()
    return "%04d-%02d-%02d".format(
        c.get(Calendar.YEAR),
        c.get(Calendar.MONTH) + 1,
        c.get(Calendar.DAY_OF_MONTH)
    )
}
