package com.flo.nem12.repository

import com.flo.nem12.model.FailureReason
import com.flo.nem12.model.FailureRecord
import java.io.Closeable

interface FailureReadingsRepository : Closeable {
    /**
     * Save a single single failed reading
     */
    fun save(failure: FailureRecord)

    /**
     * Flush any buffered data to persistent storage
     */
    fun flush()

    /**
     * getStatistics is for statistics
     * */
    fun getStatistics(): Map<FailureReason, Int>
}
