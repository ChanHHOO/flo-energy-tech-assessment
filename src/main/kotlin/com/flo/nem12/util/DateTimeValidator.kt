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
     * ISO8601 Date format: YYYYMMDD (8 characters)
     * Example: 20050301 = 2005-03-01
     */
    private val ISO8601_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

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

    /**
     * Validates ISO8601 Date format (8 characters: YYYYMMDD)
     *
     * @param dateStr Date string to validate
     * @return true if valid, false otherwise
     */
    fun isValidISO8601Date(dateStr: String): Boolean {
        if (dateStr.length != 8) return false
        if (!dateStr.all { it.isDigit() }) return false

        return try {
            ISO8601_DATE_FORMATTER.parse(dateStr)
            true
        } catch (e: DateTimeParseException) {
            false
        }
    }

    /**
     * Parses ISO8601 DateTime string to LocalDateTime
     *
     * @param dateTimeStr DateTime string in YYYYMMDDHHmm format
     * @return LocalDateTime object
     * @throws DateTimeParseException if format is invalid
     */
    fun parseISO8601DateTime(dateTimeStr: String): LocalDateTime {
        require(dateTimeStr.length == 12) { "DateTime must be 12 characters (YYYYMMDDHHmm)" }
        return LocalDateTime.parse(dateTimeStr, ISO8601_DATETIME_FORMATTER)
    }

    /**
     * Parses ISO8601 Date string
     *
     * @param dateStr Date string in YYYYMMDD format
     * @return Formatted date string
     * @throws DateTimeParseException if format is invalid
     */
    fun parseISO8601Date(dateStr: String): String {
        require(dateStr.length == 8) { "Date must be 8 characters (YYYYMMDD)" }
        ISO8601_DATE_FORMATTER.parse(dateStr)
        return dateStr
    }
}