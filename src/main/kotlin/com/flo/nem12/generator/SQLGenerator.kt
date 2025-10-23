package com.flo.nem12.generator

import com.flo.nem12.model.MeterReading
import java.io.Closeable

/**
 * Interface for SQL generation strategies
 */
interface SQLGenerator : Closeable {
    /**
     * Add a meter reading to be written as SQL
     */
    fun addReading(reading: MeterReading)

    /**
     * Flush any buffered data to output
     */
    fun flush()
}
