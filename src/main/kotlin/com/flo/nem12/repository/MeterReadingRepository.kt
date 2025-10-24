package com.flo.nem12.repository

import com.flo.nem12.model.MeterReading
import java.io.Closeable

/**
 * Repository interface for meter reading data access
 */
interface MeterReadingRepository : Closeable {
    /**
     * Save a single meter reading
     */
    fun save(reading: MeterReading)

    /**
     * Save multiple meter readings
     */
    fun saveAll(readings: List<MeterReading>) {
        readings.forEach { save(it) }
    }

    /**
     * Flush any buffered data to persistent storage
     */
    fun flush()
}
