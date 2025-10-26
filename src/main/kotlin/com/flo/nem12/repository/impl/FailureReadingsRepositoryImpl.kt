package com.flo.nem12.repository.impl

import com.flo.nem12.config.DatabaseConfig
import com.flo.nem12.model.FailureReason
import com.flo.nem12.model.FailureRecord
import com.flo.nem12.repository.FailureReadingsRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.use

private val logger = KotlinLogging.logger {}

class FailureReadingsRepositoryImpl(
    private val dbPath: Path,
    private val batchSize: Int = DatabaseConfig.DEFAULT_BATCH_SIZE
): FailureReadingsRepository {
    private val connection: Connection
    private val insertStatement: PreparedStatement
    private val timestampFormatter = DateTimeFormatter.ofPattern(DatabaseConfig.TIMESTAMP_FORMAT)
    private var batchCount = 0
    private val statistics = mutableMapOf<FailureReason, Int>()

    init {
        logger.info { "Initializing DatabaseFailureHandler at: $dbPath" }

        // Connect to SQLite database
        connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        connection.autoCommit = false

        // Create schema if not exists
        ensureSchema()

        // Prepare insert statement
        insertStatement = connection.prepareStatement(DatabaseConfig.INSERT_FAILED_READING_SQL)

        logger.info { "DatabaseFailureHandler initialized successfully" }
    }

    private fun ensureSchema() {
        connection.createStatement().use { statement ->
            statement.execute(DatabaseConfig.CREATE_FAILED_READINGS_TABLE_SQL)
            connection.commit()
        }
        logger.debug { "Failed readings table schema verified/created" }
    }

    override fun save(failure: FailureRecord) {
        // Generate UUID for primary key
        val id = UUID.randomUUID().toString()

        // Set prepared statement parameters
        insertStatement.setString(1, id)
        insertStatement.setInt(2, failure.lineNumber)
        insertStatement.setString(3, failure.nmi)
        if (failure.intervalIndex != null) {
            insertStatement.setInt(4, failure.intervalIndex)
        } else {
            insertStatement.setNull(4, java.sql.Types.INTEGER)
        }
        insertStatement.setString(5, failure.rawValue)
        insertStatement.setString(6, failure.reason.name)
        insertStatement.setString(7, failure.timestamp.format(timestampFormatter))

        // Add to batch
        insertStatement.addBatch()
        batchCount++

        // Update statistics
        statistics[failure.reason] = statistics.getOrDefault(failure.reason, 0) + 1

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
        logger.info { "Flushed all pending failures. Total failures: ${statistics.values.sum()}" }
    }

    override fun getStatistics(): Map<FailureReason, Int> {
        return statistics.toMap()
    }

    override fun close() {
        try {
            flush()
            insertStatement.close()
            connection.close()
            logger.info { "DatabaseFailureHandler closed. Total failures recorded: ${statistics.values.sum()}" }
        } catch (e: Exception) {
            logger.error(e) { "Error closing DatabaseFailureHandler" }
            throw e
        }
    }

    private fun executeBatch() {
        try {
            val results = insertStatement.executeBatch()
            connection.commit()

            val inserted = results.count { it > 0 }
            logger.debug { "Batch executed: $inserted failure records inserted (batch size: $batchCount)" }

            batchCount = 0
        } catch (e: Exception) {
            logger.error(e) { "Error executing batch, rolling back" }
            connection.rollback()
            throw e
        }
    }
}