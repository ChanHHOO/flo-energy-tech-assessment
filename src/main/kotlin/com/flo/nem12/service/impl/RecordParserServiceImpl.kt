package com.flo.nem12.service.impl

import com.flo.nem12.exception.ParseException
import com.flo.nem12.model.MeterReading
import com.flo.nem12.service.RecordParserService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class RecordParserServiceImpl: RecordParserService {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    /**
     * Parse 300 (interval data) record into list of MeterReading objects
     *
     * @param line Raw 300 record line
     * @param nmi Current NMI identifier
     * @param intervalMinutes Interval duration in minutes
     * @return List of parsed meter readings
     */
    override fun parseIntervalData(line: String, nmi: String, intervalMinutes: Int): List<MeterReading> {
        val fields = line.split(",")

        require(fields.size >= 3) { "Invalid 300 record: insufficient fields" }

        // Parse date from field 1
        val date = parseDate(fields[1])

        // Parse consumption values starting from field 2
        val readings = mutableListOf<MeterReading>()
        val expectedIntervals = calculateExpectedIntervals(intervalMinutes)
        for (i in 0 until expectedIntervals) {
            val consumptionStr = fields[i + 2]

            // Skip empty or non-numeric values
            if (consumptionStr.isBlank() || !consumptionStr.isNumeric()) {
                continue
            }

            val consumption = BigDecimal(consumptionStr)
            val timestamp = calculateIntervaTime(date, intervalMinutes, i)

            readings.add(MeterReading(nmi, timestamp, consumption))
        }

        return readings
    }

    private fun parseDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr, dateFormatter)
        } catch (e: DateTimeParseException) {
            throw ParseException(0, "Invalid date format: $dateStr (expected: yyyyMMdd)", e)
        }
    }

    private fun calculateExpectedIntervals(intervalMinutes: Int): Int {
        return 1440 / intervalMinutes  // 1 day = 1440 minutes
    }

    private fun String.isNumeric(): Boolean {
        return try {
            BigDecimal(this)
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

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
    fun calculateIntervaTime(date: LocalDate, intervalMinutes: Int, index: Int): LocalDateTime {
        val totalMinutes = intervalMinutes * (index + 1)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val adjustedDate = date.plusDays((hours / 24).toLong())
        val adjustedTime = LocalTime.of(hours % 24, minutes)

        return LocalDateTime.of(adjustedDate, adjustedTime)
    }
}