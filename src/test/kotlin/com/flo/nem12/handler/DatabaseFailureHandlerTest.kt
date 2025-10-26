package com.flo.nem12.handler

import com.flo.nem12.model.FailureReason
import com.flo.nem12.model.FailureRecord
import com.flo.nem12.repository.FailureReadingsRepository
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

class DatabaseFailureHandlerTest {
    @Test
    fun `should delegate handleFailure to repository`() {
        // Given
        val failure =
            FailureRecord(
                lineNumber = 42,
                nmi = "1234567890",
                intervalIndex = 10,
                rawValue = "-10.5",
                reason = FailureReason.NEGATIVE_VALUE,
                timestamp = LocalDateTime.of(2024, 1, 1, 12, 0),
            )
        val repository = TestFailureReadingsRepository()
        val handler = DatabaseFailureHandler(repository)

        // When
        handler.handleFailure(failure)

        // Then
        assertEquals(1, repository.savedFailures.size)
        assertEquals(failure, repository.savedFailures[0])
    }

    @Test
    fun `should handle multiple failure records`() {
        // Given
        val failures =
            listOf(
                FailureRecord(1, "NMI1", 0, "", FailureReason.EMPTY_VALUE),
                FailureRecord(2, "NMI2", 1, "abc", FailureReason.NON_NUMERIC_VALUE),
                FailureRecord(3, "NMI3", 2, "-5", FailureReason.NEGATIVE_VALUE),
            )
        val repository = TestFailureReadingsRepository()
        val handler = DatabaseFailureHandler(repository)

        // When
        failures.forEach { handler.handleFailure(it) }

        // Then
        assertEquals(3, repository.savedFailures.size)
        assertEquals(failures, repository.savedFailures)
    }

    @Test
    fun `should delegate getStatistics to repository`() {
        // Given
        val failures =
            listOf(
                FailureRecord(1, "NMI1", 0, "", FailureReason.EMPTY_VALUE),
                FailureRecord(2, "NMI2", 0, "", FailureReason.EMPTY_VALUE),
                FailureRecord(3, "NMI3", 0, "abc", FailureReason.NON_NUMERIC_VALUE),
                FailureRecord(4, "NMI4", 0, "-5", FailureReason.NEGATIVE_VALUE),
                FailureRecord(5, "NMI5", 0, "-10", FailureReason.NEGATIVE_VALUE),
                FailureRecord(6, "NMI6", 0, "-15", FailureReason.NEGATIVE_VALUE),
            )
        val repository = TestFailureReadingsRepository()
        val handler = DatabaseFailureHandler(repository)

        // When
        failures.forEach { handler.handleFailure(it) }
        val stats = handler.getStatistics()

        // Then
        assertEquals(2, stats[FailureReason.EMPTY_VALUE])
        assertEquals(1, stats[FailureReason.NON_NUMERIC_VALUE])
        assertEquals(3, stats[FailureReason.NEGATIVE_VALUE])
    }

    @Test
    fun `should handle all failure reason types`() {
        // Given
        val failures =
            FailureReason.entries.mapIndexed { index, reason ->
                FailureRecord(
                    lineNumber = index,
                    nmi = "NMI$index",
                    intervalIndex = index,
                    rawValue = "value$index",
                    reason = reason,
                )
            }
        val repository = TestFailureReadingsRepository()
        val handler = DatabaseFailureHandler(repository)

        // When
        failures.forEach { handler.handleFailure(it) }
        val stats = handler.getStatistics()

        // Then
        assertEquals(FailureReason.entries.size, repository.savedFailures.size)
        assertEquals(FailureReason.entries.size, stats.size)
        FailureReason.entries.forEach { reason ->
            assertEquals(1, stats[reason])
        }
    }

    /**
     * Test double for FailureReadingsRepository
     */
    private class TestFailureReadingsRepository : FailureReadingsRepository {
        val savedFailures = mutableListOf<FailureRecord>()
        var flushed = false
        var closed = false

        override fun save(failure: FailureRecord) {
            savedFailures.add(failure)
        }

        override fun flush() {
            flushed = true
        }

        override fun getStatistics(): Map<FailureReason, Int> {
            return savedFailures.groupingBy { it.reason }.eachCount()
        }

        override fun close() {
            closed = true
        }
    }
}
