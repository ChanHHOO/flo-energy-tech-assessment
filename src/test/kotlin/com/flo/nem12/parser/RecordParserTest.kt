package com.flo.nem12.parser

import com.flo.nem12.exception.ParseException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordParserTest {

    private val parser = RecordParser()

    @Test
    fun `should parse 30-minute interval data correctly`() {
        // Given
        val line = "300,20240101,10.5,11.2,12.3,13.4,14.5,15.6,16.7,17.8,18.9,19.0," +
                "20.1,21.2,22.3,23.4,24.5,25.6,26.7,27.8,28.9,29.0," +
                "30.1,31.2,32.3,33.4,34.5,35.6,36.7,37.8,38.9,39.0," +
                "40.1,41.2,42.3,43.4,44.5,45.6,46.7,47.8,48.9,49.0," +
                "50.1,51.2,52.3,53.4,54.5,55.6,56.7,57.8,A,,20050301120000,,"
        val nmi = "1234567890"
        val intervalMinutes = 30

        // When
        val readings = parser.parseIntervalData(line, nmi, intervalMinutes)

        // Then
        assertEquals(48, readings.size)
        assertEquals(nmi, readings[0].nmi)
        assertEquals(BigDecimal("10.5"), readings[0].consumption)
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 30), readings[0].timestamp)
        assertEquals(LocalDateTime.of(2024, 1, 1, 1, 0), readings[1].timestamp)
    }

    @Test
    fun `should parse 15-minute interval data correctly`() {
        // Given
        val intervals15Min = (0 until 96).joinToString(",") { (it + 1).toString() }
        val line = "300,20240101,$intervals15Min,A,,20050301120000,,"
        val nmi = "1234567890"
        val intervalMinutes = 15

        // When
        val readings = parser.parseIntervalData(line, nmi, intervalMinutes)

        // Then
        assertEquals(96, readings.size)
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 15), readings[0].timestamp)
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 30), readings[1].timestamp)
        assertEquals(LocalDateTime.of(2024, 1, 2, 0, 0), readings[95].timestamp)
    }

    @Test
    fun `should parse 5-minute interval data correctly`() {
        // Given
        val intervals5Min = (0 until 288).joinToString(",") { "1.0" }
        val line = "300,20240101,$intervals5Min,A,,20050301120000,,"
        val nmi = "1234567890"
        val intervalMinutes = 5

        // When
        val readings = parser.parseIntervalData(line, nmi, intervalMinutes)

        // Then
        assertEquals(288, readings.size)
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 5), readings[0].timestamp)
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 10), readings[1].timestamp)
        assertEquals(LocalDateTime.of(2024, 1, 2, 0, 0), readings[287].timestamp)
    }

    @Test
    fun `should skip empty values in interval data`() {
        // Given
        val intervals = (0 until 48).joinToString(",") { i ->
            when (i) {
                0 -> "10.5"
                1 -> ""
                2 -> "12.3"
                3 -> ""
                4 -> "14.5"
                else -> ""
            }
        }
        val line = "300,20240101,$intervals,A,,20050301120000,,"
        val nmi = "1234567890"
        val intervalMinutes = 30

        // When
        val readings = parser.parseIntervalData(line, nmi, intervalMinutes)

        // Then
        assertEquals(3, readings.size)
        assertEquals(BigDecimal("10.5"), readings[0].consumption)
        assertEquals(BigDecimal("12.3"), readings[1].consumption)
        assertEquals(BigDecimal("14.5"), readings[2].consumption)
    }

    @Test
    fun `should skip non-numeric values in interval data`() {
        // Given
        val intervals = (0 until 48).joinToString(",") { i ->
            when (i) {
                0 -> "10.5"
                1 -> "A"
                2 -> "12.3"
                3 -> "N/A"
                4 -> "14.5"
                5 -> "INVALID"
                6 -> "16.7"
                else -> ""
            }
        }
        val line = "300,20240101,$intervals,A,,20050301120000,,"
        val nmi = "1234567890"
        val intervalMinutes = 30

        // When
        val readings = parser.parseIntervalData(line, nmi, intervalMinutes)

        // Then
        assertEquals(4, readings.size)
        assertEquals(BigDecimal("10.5"), readings[0].consumption)
        assertEquals(BigDecimal("12.3"), readings[1].consumption)
        assertEquals(BigDecimal("14.5"), readings[2].consumption)
        assertEquals(BigDecimal("16.7"), readings[3].consumption)
    }

    @Test
    fun `should skip negative consumption values`() {
        // Given
        val intervals = (0 until 48).joinToString(",") { i ->
            when (i) {
                0 -> "-10.5"
                1 -> "11.2"
                2 -> "-12.3"
                3 -> "15.8"
                else -> ""
            }
        }
        val line = "300,20240101,$intervals,A,,20050301120000,,"
        val nmi = "1234567890"
        val intervalMinutes = 30

        // When
        val readings = parser.parseIntervalData(line, nmi, intervalMinutes)

        // Then
        assertEquals(2, readings.size)
        assertEquals(BigDecimal("11.2"), readings[0].consumption)
        assertEquals(BigDecimal("15.8"), readings[1].consumption)
    }

    @Test
    fun `should handle zero consumption values`() {
        // Given
        val intervals = (0 until 48).joinToString(",") { i ->
            when (i) {
                0 -> "0.0"
                1 -> "0"
                2 -> "0.00"
                else -> ""
            }
        }
        val line = "300,20240101,$intervals,A,,20050301120000,,"
        val nmi = "1234567890"
        val intervalMinutes = 30

        // When
        val readings = parser.parseIntervalData(line, nmi, intervalMinutes)

        // Then
        assertEquals(3, readings.size)
        assertTrue(readings.all { it.consumption.compareTo(BigDecimal.ZERO) == 0 })
    }

    @Test
    fun `should throw exception for invalid date format`() {
        // Given
        val intervals = (0 until 48).joinToString(",") { "10.5" }
        val line = "300,2024-01-01,$intervals,A,,20050301120000,,"
        val nmi = "1234567890"
        val intervalMinutes = 30

        // When & Then
        assertThrows<ParseException> {
            parser.parseIntervalData(line, nmi, intervalMinutes)
        }
    }

    @Test
    fun `should throw exception for insufficient fields`() {
        // Given
        val line = "300,20240101"
        val nmi = "1234567890"
        val intervalMinutes = 30

        // When & Then
        assertThrows<IllegalArgumentException> {
            parser.parseIntervalData(line, nmi, intervalMinutes)
        }
    }

    @Test
    fun `should handle valid decimal precision within 4 digits`() {
        // Given
        val intervals = (0 until 48).joinToString(",") { i ->
            when (i) {
                0 -> "10.1234"
                1 -> "11.9876"
                else -> ""
            }
        }
        val line = "300,20240101,$intervals,A,,20050301120000,,"
        val nmi = "1234567890"
        val intervalMinutes = 30

        // When
        val readings = parser.parseIntervalData(line, nmi, intervalMinutes)

        // Then
        assertEquals(2, readings.size)
        assertEquals(BigDecimal("10.1234"), readings[0].consumption)
        assertEquals(BigDecimal("11.9876"), readings[1].consumption)
    }

    @Test
    fun `should preserve NMI identifier in all readings`() {
        // Given
        val intervals = (0 until 48).joinToString(",") { i ->
            when (i) {
                0 -> "10.5"
                1 -> "11.2"
                2 -> "12.3"
                else -> ""
            }
        }
        val line = "300,20240101,$intervals,A,,20050301120000,,"
        val nmi = "ABC1234567"
        val intervalMinutes = 30

        // When
        val readings = parser.parseIntervalData(line, nmi, intervalMinutes)

        // Then
        assertEquals(3, readings.size)
        assertTrue(readings.all { it.nmi == nmi })
    }

    @Test
    fun `should skip consumption with more than 15 integer digits`() {
        // Given
        val intervals = (0 until 48).joinToString(",") { i ->
            when (i) {
                0 -> "1234567890123456"
                1 -> "10.5"
                2 -> "9999999999999999.1234"
                3 -> "20.3"
                else -> ""
            }
        }
        val line = "300,20240101,$intervals,A,,20050301120000,,"
        val nmi = "1234567890"
        val intervalMinutes = 30

        // When
        val readings = parser.parseIntervalData(line, nmi, intervalMinutes)

        // Then
        assertEquals(2, readings.size)
        assertEquals(BigDecimal("10.5"), readings[0].consumption)
        assertEquals(BigDecimal("20.3"), readings[1].consumption)
    }

    @Test
    fun `should skip consumption with more than 4 decimal digits`() {
        // Given
        val intervals = (0 until 48).joinToString(",") { i ->
            when (i) {
                0 -> "123.12345"
                1 -> "10.5"
                2 -> "0.123456"
                3 -> "20.1234"
                else -> ""
            }
        }
        val line = "300,20240101,$intervals,A,,20050301120000,,"
        val nmi = "1234567890"
        val intervalMinutes = 30

        // When
        val readings = parser.parseIntervalData(line, nmi, intervalMinutes)

        // Then
        assertEquals(2, readings.size)
        assertEquals(BigDecimal("10.5"), readings[0].consumption)
        assertEquals(BigDecimal("20.1234"), readings[1].consumption)
    }
}