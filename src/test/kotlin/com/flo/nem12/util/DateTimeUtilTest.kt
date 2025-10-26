package com.flo.nem12.util

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

class DateTimeUtilTest {
    @Test
    fun `should convert AEST to UTC correctly`() {
        // Given - Winter time (AEST = UTC+10)
        val aestTime = LocalDateTime.of(2024, 7, 1, 14, 30, 0)

        // When
        val utcTime = DateTimeUtil.aestToUtc(aestTime)

        // Then
        val expected = LocalDateTime.of(2024, 7, 1, 4, 30, 0)
        assertEquals(expected, utcTime)
    }

    @Test
    fun `should convert AEDT to UTC correctly during DST`() {
        // Given - Summer time (AEDT = UTC+11)
        val aedtTime = LocalDateTime.of(2024, 1, 15, 14, 30, 0)

        // When
        val utcTime = DateTimeUtil.aestToUtc(aedtTime)

        // Then
        val expected = LocalDateTime.of(2024, 1, 15, 3, 30, 0)
        assertEquals(expected, utcTime)
    }

    @Test
    fun `should handle midnight conversion correctly`() {
        // Given
        val aestMidnight = LocalDateTime.of(2024, 7, 1, 0, 0, 0)

        // When
        val utcTime = DateTimeUtil.aestToUtc(aestMidnight)

        // Then - Previous day in UTC
        val expected = LocalDateTime.of(2024, 6, 30, 14, 0, 0)
        assertEquals(expected, utcTime)
    }
}
