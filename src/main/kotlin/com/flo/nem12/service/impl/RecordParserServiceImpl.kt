package com.flo.nem12.service.impl

import com.flo.nem12.handler.FailureHandler
import com.flo.nem12.model.FailureReason
import com.flo.nem12.model.FailureRecord
import com.flo.nem12.model.MeterReading
import com.flo.nem12.service.RecordParserService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.IllegalArgumentException

class RecordParserServiceImpl(
    private val failureHandler: FailureHandler,
) : RecordParserService {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    private var currentLineNumber: Int = 0

    /**
     * Parse 300 (interval data) record into list of MeterReading objects
     *
     * @param line Raw 300 record line
     * @param nmi Current NMI identifier
     * @param intervalMinutes Interval duration in minutes
     * @return List of parsed meter readings
     */
    override fun parseIntervalData(
        line: String,
        nmi: String,
        intervalMinutes: Int,
    ): List<MeterReading> {
        val fields = line.split(",")
        val expectedIntervals = calculateExpectedIntervals(intervalMinutes)

        validateFields(fields, expectedIntervals, nmi)

        // Parse date from field 1
        val date = parseDate(fields[1], nmi) ?: return emptyList()

        // Parse consumption values starting from field 2
        val readings = mutableListOf<MeterReading>()
        failureHandler.use {
            for (i in 0 until expectedIntervals) {
                val timestamp = calculateIntervaTime(date, intervalMinutes, i)

                /**
                 * This validation logic is to cause Typecast to be called only once.
                 * */
                val consumptionStr = fields[i + 2]
                if (!isValidConsumptionStr(consumptionStr, nmi, i, timestamp)) continue

                val consumption = BigDecimal(consumptionStr)
                if (!isValidConsumption(consumption, nmi, i, timestamp)) continue

                readings.add(MeterReading(nmi, timestamp, consumption))
            }
        }
        return readings
    }

    override fun setLineNumber(lineNumber: Int) {
        this.currentLineNumber = lineNumber
    }

    private fun isValidConsumptionStr(
        consumptionStr: String,
        nmi: String,
        intervalIndex: Int,
        timestamp: LocalDateTime,
    ): Boolean {
        // Skip empty or non-numeric values

        // Handle empty values
        if (consumptionStr.isBlank()) {
            failureHandler.handleFailure(
                FailureRecord(
                    lineNumber = currentLineNumber,
                    reason = FailureReason.EMPTY_VALUE,
                    nmi = nmi,
                    intervalIndex = intervalIndex,
                    rawValue = consumptionStr,
                    timestamp = timestamp,
                ),
            )
            return false
        }

        // Handle non-numeric values
        if (!consumptionStr.isNumeric()) {
            failureHandler.handleFailure(
                FailureRecord(
                    lineNumber = currentLineNumber,
                    reason = FailureReason.NON_NUMERIC_VALUE,
                    nmi = nmi,
                    intervalIndex = intervalIndex,
                    rawValue = consumptionStr,
                    timestamp = timestamp,
                ),
            )
            return false
        }
        return true
    }

    private fun isValidConsumption(
        consumption: BigDecimal,
        nmi: String,
        intervalIndex: Int,
        timestamp: LocalDateTime,
    ): Boolean {
        // 1. Skip negative values
        // 2. Validate consumption format (15.4: max 15 integer digits, max 4 decimal digits)

        // Handle negative values
        if (consumption < BigDecimal.ZERO) {
            failureHandler.handleFailure(
                FailureRecord(
                    lineNumber = currentLineNumber,
                    reason = FailureReason.NEGATIVE_VALUE,
                    nmi = nmi,
                    intervalIndex = intervalIndex,
                    rawValue = consumption.toString(),
                    timestamp = timestamp,
                ),
            )
            return false
        }

        // Handle invalid decimal scale
        if (!isValidConsumptionFormat(consumption)) {
            failureHandler.handleFailure(
                FailureRecord(
                    lineNumber = currentLineNumber,
                    reason = FailureReason.INVALID_CONSUMPTION_FORMAT,
                    nmi = nmi,
                    intervalIndex = intervalIndex,
                    rawValue = consumption.toString(),
                    timestamp = timestamp,
                ),
            )
            return false
        }

        return true
    }

    private fun validateFields(
        fields: List<String?>,
        expectedIntervals: Int,
        nmi: String,
    ) {
        val actualIntervals = fields.size - 7 // The number of record fields without interval values.

        if (actualIntervals != expectedIntervals) {
            failureHandler.handleFailure(
                FailureRecord(
                    lineNumber = currentLineNumber,
                    reason = FailureReason.INTERVAL_COUNT_MISMATCH,
                    nmi = nmi,
                    intervalIndex = null,
                    rawValue = "Expected $expectedIntervals intervals but found $actualIntervals",
                    timestamp = null,
                ),
            )
            throw IllegalArgumentException("Expected $expectedIntervals intervals but found $actualIntervals")
        }
        /**
         * The number of Mandatory Fields in Interval data record is 3
         * RecordIndicator, IntervalDate, IntervalValue(at least one), QualityMethod
         * */
        if (fields.size < 4) {
            failureHandler.handleFailure(
                FailureRecord(
                    lineNumber = currentLineNumber,
                    reason = FailureReason.INVALID_FIELDS,
                    nmi = nmi,
                    intervalIndex = null,
                    rawValue = "",
                    timestamp = null,
                ),
            )
            throw IllegalArgumentException("Line number must greater than 3")
        }
    }

    private fun parseDate(
        dateStr: String,
        nmi: String,
    ): LocalDate? {
        return try {
            LocalDate.parse(dateStr, dateFormatter)
        } catch (_: DateTimeParseException) {
            /**
             * If date is null, retry logic should refer line number field.
             * Retry logic should try to save for all the interval data in line.
             * */
            failureHandler.handleFailure(
                FailureRecord(
                    lineNumber = currentLineNumber,
                    reason = FailureReason.INVALID_DATE_FORMAT,
                    nmi = nmi,
                    intervalIndex = null,
                    rawValue = dateStr,
                ),
            )
            return null
        }
    }

    private fun calculateExpectedIntervals(intervalMinutes: Int): Int {
        return 1440 / intervalMinutes // 1 day = 1440 minutes
    }

    private fun String.isNumeric(): Boolean {
        return try {
            BigDecimal(this)
            true
        } catch (_: NumberFormatException) {
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
    private fun calculateIntervaTime(
        date: LocalDate,
        intervalMinutes: Int,
        index: Int,
    ): LocalDateTime {
        val totalMinutes = intervalMinutes * (index + 1)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val adjustedDate = date.plusDays((hours / 24).toLong())
        val adjustedTime = LocalTime.of(hours % 24, minutes)

        return LocalDateTime.of(adjustedDate, adjustedTime)
    }
}
