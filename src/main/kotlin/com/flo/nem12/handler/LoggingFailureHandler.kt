package com.flo.nem12.handler

import com.flo.nem12.model.FailureReason
import com.flo.nem12.model.FailureRecord
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Failure handler that logs failures to console
 * Does not persist failures, only logs them for monitoring and debugging
 */
class LoggingFailureHandler : FailureHandler {
    private val logger = KotlinLogging.logger {}
    private val statistics = mutableMapOf<FailureReason, Int>()

    override fun handleFailure(failure: FailureRecord) {
        // Log the failure
        logger.warn {
            buildString {
                append("Parsing failure - ")
                append("Line ${failure.lineNumber}")
                append("${failure.reason} ")
                append("(NMI: ${failure.nmi}")
                failure.intervalIndex?.let { append(", Interval: $it") }
                failure.timestamp?.let { append(", Time: $it") }
                append(", Raw: '${failure.rawValue}')")
            }
        }

        // Update statistics
        statistics[failure.reason] = statistics.getOrDefault(failure.reason, 0) + 1
    }

    override fun getStatistics(): Map<FailureReason, Int> {
        return statistics.toMap()
    }

    override fun close() {
        if (statistics.isNotEmpty()) {
            logger.info { "Logging handler closed. Total failures: ${statistics.values.sum()}" }
        }
    }
}
