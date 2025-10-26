package com.flo.nem12.repository

import com.flo.nem12.model.MeterReading
import java.io.Closeable

/**
 * Repository interface for meter reading data access
 * Default implementation is provided by BaseSQLiteRepository
 */
interface MeterReadingRepository : Closeable {
    /**
     * Save a single meter reading
     * Implementation is provided by BaseSQLiteRepository
     */
    fun save(entity: MeterReading)

    /**
     * Flush any buffered data to persistent storage
     * Implementation is provided by BaseSQLiteRepository
     */
    fun flush()
}
