package com.aplivit.infrastructure

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.timeIntervalSince1970

actual fun nowEpochSeconds(): Long = NSDate().timeIntervalSince1970.toLong()

actual fun todayIsoDate(): String {
    val fmt = NSDateFormatter()
    fmt.dateFormat = "yyyy-MM-dd"
    return fmt.stringFromDate(NSDate())
}
