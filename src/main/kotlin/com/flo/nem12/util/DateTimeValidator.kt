package com.flo.nem12.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Utility class for validating and parsing NEM12 date/time formats
 */
object DateTimeValidator {
    /**
     * NEM12 DateTime format: YYYYMMDDHHmmss (12 characters)
     * Example: 200506081149 = 2005-06-08 11:49:00
     */
    private val NEM12_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

    /**
     * NEM12 Date format: YYYYMMDD (8 characters)
     * Example: 20050301 = 2005-03-01
     */
    private val NEM12_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

    /**
     * Validates NEM12 DateTime format (12 characters: YYYYMMDDHHmm)
     *
     * @param dateTimeStr DateTime string to validate
     * @return true if valid, false otherwise
     */
    fun isValidNEM12DateTime(dateTimeStr: String): Boolean {
        if (dateTimeStr.length != 12) return false
        if (!dateTimeStr.all { it.isDigit() }) return false

        return try {
            LocalDateTime.parse(dateTimeStr, NEM12_DATETIME_FORMATTER)
            true
        } catch (e: DateTimeParseException) {
            false
        }
    }

    /**
     * Validates NEM12 Date format (8 characters: YYYYMMDD)
     *
     * @param dateStr Date string to validate
     * @return true if valid, false otherwise
     */
    fun isValidNEM12Date(dateStr: String): Boolean {
        if (dateStr.length != 8) return false
        if (!dateStr.all { it.isDigit() }) return false

        return try {
            NEM12_DATE_FORMATTER.parse(dateStr)
            true
        } catch (e: DateTimeParseException) {
            false
        }
    }

    /**
     * Parses NEM12 DateTime string to LocalDateTime
     *
     * @param dateTimeStr DateTime string in YYYYMMDDHHmm format
     * @return LocalDateTime object
     * @throws DateTimeParseException if format is invalid
     */
    fun parseNEM12DateTime(dateTimeStr: String): LocalDateTime {
        require(dateTimeStr.length == 12) { "DateTime must be 12 characters (YYYYMMDDHHmm)" }
        return LocalDateTime.parse(dateTimeStr, NEM12_DATETIME_FORMATTER)
    }

    /**
     * Parses NEM12 Date string
     *
     * @param dateStr Date string in YYYYMMDD format
     * @return Formatted date string
     * @throws DateTimeParseException if format is invalid
     */
    fun parseNEM12Date(dateStr: String): String {
        require(dateStr.length == 8) { "Date must be 8 characters (YYYYMMDD)" }
        NEM12_DATE_FORMATTER.parse(dateStr)
        return dateStr
    }
}