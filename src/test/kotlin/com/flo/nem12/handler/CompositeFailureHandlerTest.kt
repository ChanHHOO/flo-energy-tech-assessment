package com.flo.nem12.handler

import com.flo.nem12.model.FailureReason
import com.flo.nem12.model.FailureRecord
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

class CompositeFailureHandlerTest {
    @Test
    fun `should delegate to all handlers`() {
        // Given
        val loggingHandler = LoggingFailureHandler()
        val compositeHandler = CompositeFailureHandler(loggingHandler)

        val failure =
            FailureRecord(
                lineNumber = 10,
                reason = FailureReason.NEGATIVE_VALUE,
                nmi = "1234567890",
                intervalIndex = 5,
                rawValue = "-10.5",
                timestamp = LocalDateTime.of(2024, 1, 1, 12, 0),
            )

        // When
        compositeHandler.handleFailure(failure)

        // Then
        val stats = compositeHandler.getStatistics()
        assertEquals(1, stats[FailureReason.NEGATIVE_VALUE])

        // Clean up
        compositeHandler.close()
    }

    @Test
    fun `should return statistics from first handler`() {
        // Given
        val handler1 = LoggingFailureHandler()
        val handler2 = LoggingFailureHandler()
        val compositeHandler = CompositeFailureHandler(handler1, handler2)

        val failure1 =
            FailureRecord(
                lineNumber = 10,
                reason = FailureReason.NEGATIVE_VALUE,
                nmi = "1234567890",
                intervalIndex = 5,
                rawValue = "-10.5",
                timestamp = LocalDateTime.of(2024, 1, 1, 12, 0),
            )

        val failure2 =
            FailureRecord(
                lineNumber = 11,
                reason = FailureReason.EMPTY_VALUE,
                nmi = "1234567890",
                intervalIndex = 6,
                rawValue = "",
                timestamp = LocalDateTime.of(2024, 1, 1, 12, 30),
            )

        // When
        compositeHandler.handleFailure(failure1)
        compositeHandler.handleFailure(failure2)

        // Then - Statistics should come from first handler only (to avoid double counting)
        val stats = compositeHandler.getStatistics()
        assertEquals(1, stats[FailureReason.NEGATIVE_VALUE])
        assertEquals(1, stats[FailureReason.EMPTY_VALUE])

        // Clean up
        compositeHandler.close()
    }
}
