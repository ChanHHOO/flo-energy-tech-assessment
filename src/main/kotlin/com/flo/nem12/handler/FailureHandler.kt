package com.flo.nem12.handler

import com.flo.nem12.model.FailureReason
import com.flo.nem12.model.FailureRecord
import java.io.Closeable

/**
 * Interface for handling parsing failures
 * Implementations can choose how to process failure records (e.g., database, logging, etc.)
 */
interface FailureHandler: Closeable {
    /**
     * Handle a single failure record
     *
     * @param failure The failure record to handle
     */
    fun handleFailure(failure: FailureRecord)

    /**
     * Get failure statistics grouped by reason
     *
     * @return Map of FailureReason to count
     */
    fun getStatistics(): Map<FailureReason, Int>
}
