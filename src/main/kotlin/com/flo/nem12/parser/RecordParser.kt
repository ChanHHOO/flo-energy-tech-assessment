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

        /**
         * The number of Mandatory Fields in Interval data record is 3
         * RecordIndicator, IntervalDate, IntervalValue(at least one), QualityMethod
         * */
        require(fields.size >= 4) { "Invalid 300 record: insufficient fields" }

        // Parse date from field 1
        val date = parseDate(fields[1])

        // Parse consumption values starting from field 2
        val readings = mutableListOf<MeterReading>()
        val expectedIntervals = calculateExpectedIntervals(intervalMinutes)
        for (i in 0 until expectedIntervals) {
            /**
             * This validation logic is to cause Typecast to be called only once.
             * */
            val consumptionStr = fields[i + 2]
            if(!isValidConsumptionStr(consumptionStr)) continue

            val consumption = BigDecimal(consumptionStr)
            if (!isValidConsumption(consumption)) continue

            val timestamp = timestampCalculator.calculate(date, intervalMinutes, i)

            readings.add(MeterReading(nmi, timestamp, consumption))
        }

        return readings
    }

    private fun isValidConsumptionStr(consumptionStr: String): Boolean{
        // Skip empty or non-numeric values
        return consumptionStr.isNotBlank() && consumptionStr.isNumeric()
    }

    private fun isValidConsumption(consumption: BigDecimal): Boolean {
        // 1. Skip negative values
        // 2. Validate consumption format (15.4: max 15 integer digits, max 4 decimal digits)
        return consumption >= BigDecimal.ZERO && isValidConsumptionFormat(consumption)
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
     * Validate consumption format according to NEM12 specification
     * Format: 15.4 (max 15 integer digits, max 4 decimal digits)
     *
     * @param consumption Consumption value to validate
     * @return true if format is valid, false otherwise
     */
    private fun isValidConsumptionFormat(consumption: BigDecimal): Boolean {
        val scale = consumption.scale()
        val precision = consumption.precision()

        if (scale > 4) {
            return false
        }

        val integerDigits = precision - scale
        return integerDigits <= 15
    }
}
