package com.flo.nem12.generator

import com.flo.nem12.config.DatabaseConfig
import com.flo.nem12.model.MeterReading
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.time.format.DateTimeFormatter
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * SQLite database generator with direct JDBC insertion
 * Uses batch processing for optimal performance
 */
class SQLiteGenerator(
    private val dbPath: Path,
    private val batchSize: Int = DatabaseConfig.DEFAULT_BATCH_SIZE
) : SQLGenerator {

    private val connection: Connection
    private val insertStatement: PreparedStatement
    private val timestampFormatter = DateTimeFormatter.ofPattern(DatabaseConfig.TIMESTAMP_FORMAT)
    private var batchCount = 0
    private var totalInserted = 0

    init {
        logger.info { "Initializing SQLite database at: $dbPath" }

        // Connect to SQLite database
        connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
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

    override fun addReading(reading: MeterReading) {
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
            connection.close()
            logger.info { "SQLiteGenerator closed. Total records: $totalInserted" }
        } catch (e: Exception) {
            logger.error(e) { "Error closing SQLiteGenerator" }
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
