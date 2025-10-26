package com.flo.nem12.repository.impl

import com.flo.nem12.config.DatabaseConfig
import com.flo.nem12.model.MeterReading
import com.flo.nem12.repository.MeterReadingRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import java.sql.Connection
import java.sql.PreparedStatement
import java.time.format.DateTimeFormatter
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Implementation of MeterReadingRepository
 * Uses batch processing for optimal performance
 */
class MeterReadingRepositoryImpl(
    private val connection: Connection,
    private val batchSize: Int = DatabaseConfig.DEFAULT_BATCH_SIZE,
) : MeterReadingRepository {
    private val insertStatement: PreparedStatement
    private val timestampFormatter = DateTimeFormatter.ofPattern(DatabaseConfig.TIMESTAMP_FORMAT)
    private var batchCount = 0
    private var totalInserted = 0

    init {
        logger.info { "Initializing SQLite database" }

        // Connect to SQLite database
        connection.autoCommit = false // Use manual transactions for better performance

        // Create schema if not exists
        ensureSchema()

        // Prepare insert statement
        insertStatement = connection.prepareStatement(DatabaseConfig.INSERT_SQL)

        logger.info { "SQLite database initialized successfully" }
    }

    private fun ensureSchema() {
        connection.createStatement().use { statement ->
            statement.execute(DatabaseConfig.CREATE_TABLE_SQL)
            connection.commit()
        }
        logger.debug { "Database schema verified/created" }
    }

    override fun save(reading: MeterReading) {
        // Generate UUID for primary key
        val id = UUID.randomUUID().toString()

        // Set prepared statement parameters
        insertStatement.setString(1, id)
        insertStatement.setString(2, reading.nmi)
        insertStatement.setString(3, reading.timestamp.format(timestampFormatter))
        insertStatement.setString(4, reading.consumption.toPlainString())

        // Add to batch
        insertStatement.addBatch()
        batchCount++

        // Execute batch when batch size is reached
        if (batchCount >= batchSize) {
            executeBatch()
        }
    }

    override fun flush() {
        if (batchCount > 0) {
            executeBatch()
        }
        connection.commit()
        logger.info { "Flushed all pending data. Total records inserted: $totalInserted" }
    }

    override fun close() {
        try {
            flush()
            insertStatement.close()
            logger.info { "MeterReadingRepository closed. Total records: $totalInserted" }
        } catch (e: Exception) {
            logger.error(e) { "Error closing MeterReadingRepository" }
            throw e
        }
    }

    private fun executeBatch() {
        try {
            val results = insertStatement.executeBatch()
            connection.commit()

            val inserted = results.count { it > 0 }
            totalInserted += inserted

            logger.debug { "Batch executed: $inserted records inserted (batch size: $batchCount)" }

            batchCount = 0
        } catch (e: Exception) {
            logger.error(e) { "Error executing batch, rolling back" }
            connection.rollback()
            throw e
        }
    }
}
