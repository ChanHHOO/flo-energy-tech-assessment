package com.flo.nem12.handler

import com.flo.nem12.model.FailureReason
import com.flo.nem12.model.FailureRecord

/**
 * Composite failure handler that delegates to multiple handlers
 * Allows combining different failure handling strategies (e.g., database + logging)
 *
 * @param handlers List of handlers to delegate to
 */
class CompositeFailureHandler(
    private val handlers: List<FailureHandler>,
) : FailureHandler {
    constructor(vararg handlers: FailureHandler) : this(handlers.toList())

    override fun handleFailure(failure: FailureRecord) {
        handlers.forEach { handler ->
            try {
                handler.handleFailure(failure)
            } catch (e: Exception) {
                // Log error but continue with other handlers
                System.err.println("Error in handler ${handler::class.simpleName}: ${e.message}")
            }
        }
    }

    override fun getStatistics(): Map<FailureReason, Int> {
        // Aggregate statistics from all handlers
        val aggregated = mutableMapOf<FailureReason, Int>()

        handlers.forEach { handler ->
            handler.getStatistics().forEach { (reason, count) ->
                aggregated[reason] = aggregated.getOrDefault(reason, 0) + count
            }
        }

        return aggregated
    }

    override fun close() {
        handlers.forEach { handler ->
            try {
                handler.close()
            } catch (e: Exception) {
                System.err.println("Error closing handler ${handler::class.simpleName}: ${e.message}")
            }
        }
    }
}
