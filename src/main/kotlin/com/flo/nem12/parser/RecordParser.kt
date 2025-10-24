package com.flo.nem12.parser

import com.flo.nem12.exception.ParseException
import com.flo.nem12.model.MeterReading
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Parses individual NEM12 record lines
 */
class RecordParser {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val timestampCalculator = TimestampCalculator()

    /**
     * Parse 300 (interval data) record into list of MeterReading objects
     *
     * @param line Raw 300 record line
     * @param nmi Current NMI identifier
     * @param intervalMinutes Interval duration in minutes
     * @return List of parsed meter readings
     */
    fun parseIntervalData(line: String, nmi: String, intervalMinutes: Int): List<MeterReading> {
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

            // Skip negative values
            if (consumption < BigDecimal.ZERO) {
                continue
            }

            val timestamp = timestampCalculator.calculate(date, intervalMinutes, i)

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
}
