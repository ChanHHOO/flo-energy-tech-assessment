package com.flo.nem12.util

import java.time.LocalDateTime
import java.time.ZoneId

class DateTimeUtil {
    companion object {
        private val AEST = ZoneId.of("Australia/Sydney")
        private val UTC = ZoneId.of("UTC")

        fun aestToUtc(timestamp: LocalDateTime): LocalDateTime {
            return timestamp.atZone(AEST).withZoneSameInstant(UTC).toLocalDateTime()
        }
    }
}
