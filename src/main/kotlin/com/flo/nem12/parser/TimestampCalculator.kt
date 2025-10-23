package com.flo.nem12.parser

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Utility for calculating timestamps from interval data
 */
class TimestampCalculator {

    /**
     * Calculate timestamp from date and interval index
     *
     * @param date Base date
     * @param intervalMinutes Interval duration in minutes (e.g., 30 for 30-minute intervals)
     * @param index Index starting from 0
     * @return LocalDateTime representing the timestamp
     *
     * Examples:
     * - calculate(2005-03-01, 30, 0) → 2005-03-01T00:00:00
     * - calculate(2005-03-01, 30, 1) → 2005-03-01T00:30:00
     * - calculate(2005-03-01, 30, 47) → 2005-03-01T23:30:00
     */
    fun calculate(date: LocalDate, intervalMinutes: Int, index: Int): LocalDateTime {
        val totalMinutes = intervalMinutes * (index + 1)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val adjustedDate = date.plusDays((hours / 24).toLong())
        val adjustedTime = LocalTime.of(hours % 24, minutes)

        return LocalDateTime.of(adjustedDate, adjustedTime)
    }
}
