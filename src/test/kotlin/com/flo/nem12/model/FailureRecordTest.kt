package com.flo.nem12.model

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FailureRecordTest {

    @Test
    fun `should create FailureRecord with all fields`() {
        // Given
        val lineNumber = 42
        val nmi = "1234567890"
        val intervalIndex = 10
        val rawValue = "-10.5"
        val reason = FailureReason.NEGATIVE_VALUE
        val timestamp = LocalDateTime.of(2024, 1, 1, 12, 0)

        // When
        val record = FailureRecord(
            lineNumber = lineNumber,
            nmi = nmi,
            intervalIndex = intervalIndex,
            rawValue = rawValue,
            reason = reason,
            timestamp = timestamp
        )

        // Then
        assertEquals(lineNumber, record.lineNumber)
        assertEquals(nmi, record.nmi)
        assertEquals(intervalIndex, record.intervalIndex)
        assertEquals(rawValue, record.rawValue)
        assertEquals(reason, record.reason)
        assertEquals(timestamp, record.timestamp)
    }

    @Test
    fun `should create FailureRecord with null NMI`() {
        // Given
        val record = FailureRecord(
            lineNumber = 1,
            nmi = null,
            intervalIndex = null,
            rawValue = "invalid",
            reason = FailureReason.INVALID_DATE_FORMAT
        )

        // Then
        assertNull(record.nmi)
        assertNull(record.intervalIndex)
    }

    @Test
    fun `should create FailureRecord with null timestamp when not provided`() {
        // When
        val record = FailureRecord(
            lineNumber = 1,
            nmi = "1234567890",
            intervalIndex = 0,
            rawValue = "",
            reason = FailureReason.EMPTY_VALUE
        )

        // Then
        assertNull(record.timestamp)
    }

    @Test
    fun `should support all FailureReason types`() {
        // Given
        val reasons = FailureReason.entries

        // When & Then
        reasons.forEach { reason ->
            val record = FailureRecord(
                lineNumber = 1,
                nmi = "test",
                intervalIndex = 0,
                rawValue = "test",
                reason = reason
            )
            assertEquals(reason, record.reason)
        }
    }
}
