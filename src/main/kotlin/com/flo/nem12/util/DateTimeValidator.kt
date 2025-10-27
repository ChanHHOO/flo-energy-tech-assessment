package com.flo.nem12.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Utility class for validating and parsing NEM12 date/time formats
 */
object DateTimeValidator {
    /**
     * ISO8601 DateTime format: YYYYMMDDHHmmss (12 characters)
     * Example: 200506081149 = 2005-06-08 11:49:00
     */
    private val ISO8601_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

    /**
     * Validates ISO8601 DateTime format (12 characters: YYYYMMDDHHmm)
     *
     * @param dateTimeStr DateTime string to validate
     * @return true if valid, false otherwise
     */
    fun isValidISO8601DateTime(dateTimeStr: String): Boolean {
        if (dateTimeStr.length != 12) return false
        if (!dateTimeStr.all { it.isDigit() }) return false

        return try {
            LocalDateTime.parse(dateTimeStr, ISO8601_DATETIME_FORMATTER)
            true
        } catch (e: DateTimeParseException) {
            false
        }
    }
}
