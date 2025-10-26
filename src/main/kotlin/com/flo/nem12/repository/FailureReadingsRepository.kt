package com.flo.nem12.repository

import com.flo.nem12.model.FailureReason
import com.flo.nem12.model.FailureRecord
import java.io.Closeable

/**
 * Repository interface for failure reading data access
 * Default implementation is provided by BaseSQLiteRepository
 */
interface FailureReadingsRepository : Closeable {
    /**
     * Save a single failed reading
     * Implementation is provided by BaseSQLiteRepository
     */
    fun save(entity: FailureRecord)

    /**
     * Flush any buffered data to persistent storage
     * Implementation is provided by BaseSQLiteRepository
     */
    fun flush()

    /**
     * Get failure statistics grouped by reason
     */
    fun getStatistics(): Map<FailureReason, Int>
}
