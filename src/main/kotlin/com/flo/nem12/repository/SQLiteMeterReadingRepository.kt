package com.flo.nem12.repository

import com.flo.nem12.generator.SQLGenerator
import com.flo.nem12.model.MeterReading

/**
 * SQLite implementation of MeterReadingRepository
 * Delegates to SQLGenerator for actual database operations
 */
class SQLiteMeterReadingRepository(
    private val sqlGenerator: SQLGenerator
) : MeterReadingRepository {

    override fun save(reading: MeterReading) {
        sqlGenerator.addReading(reading)
    }

    override fun flush() {
        sqlGenerator.flush()
    }

    override fun close() {
        sqlGenerator.close()
    }
}
