package com.flo.nem12.repository

import com.flo.nem12.config.DatabaseConfig
import io.github.oshai.kotlinlogging.KLogger
import java.io.Closeable
import java.sql.Connection
import java.sql.PreparedStatement
import java.time.format.DateTimeFormatter

/**
 * Base abstract class for SQLite repositories
 * Provides common functionality for batch processing, connection management, and timezone handling
 *
 * @param T The entity type this repository handles
 */
abstract class BaseSQLiteRepository<T>(
    protected val connection: Connection,
    protected val batchSize: Int = DatabaseConfig.DEFAULT_BATCH_SIZE,
) : Closeable {
    protected val insertStatement: PreparedStatement
    protected val timestampFormatter = DateTimeFormatter.ofPattern(DatabaseConfig.TIMESTAMP_FORMAT)
    protected var batchCount = 0

    init {
        getLogger().info { "Initializing ${this::class.simpleName}" }

        // Connect to SQLite database
        connection.autoCommit = false

        // Create schema if not exists
        ensureSchema()

        // Prepare insert statement
        insertStatement = connection.prepareStatement(getInsertSql())

        getLogger().info { "${this::class.simpleName} initialized successfully" }
    }

    /**
     * Ensure database schema exists
     */
    private fun ensureSchema() {
        connection.createStatement().use { statement ->
            statement.execute(getCreateTableSql())
            connection.commit()
        }
        getLogger().debug { "Database schema verified/created" }
    }

    /**
     * Save entity to database using batch processing
     */
    fun save(entity: T) {
        bindParameters(insertStatement, entity)
        insertStatement.addBatch()
        batchCount++

        if (batchCount >= batchSize) {
            executeBatch()
        }
    }

    /**
     * Flush any buffered data to persistent storage
     */
    fun flush() {
        if (batchCount > 0) {
            executeBatch()
        }
        connection.commit()
        onFlushComplete()
    }

    /**
     * Close repository and release resources
     */
    override fun close() {
        try {
            flush()
            insertStatement.close()
            onCloseComplete()
        } catch (e: Exception) {
            getLogger().error(e) { "Error closing ${this::class.simpleName}" }
            throw e
        }
    }

    /**
     * Execute batch insert
     */
    private fun executeBatch() {
        try {
            val results = insertStatement.executeBatch()
            connection.commit()

            val inserted = results.count { it > 0 }
            onBatchExecuted(inserted, batchCount)

            getLogger().debug { "Batch executed: $inserted records inserted (batch size: $batchCount)" }

            batchCount = 0
        } catch (e: Exception) {
            getLogger().error(e) { "Error executing batch, rolling back" }
            connection.rollback()
            throw e
        }
    }

    // Abstract methods to be implemented by subclasses

    protected abstract fun getCreateTableSql(): String

    protected abstract fun getInsertSql(): String

    protected abstract fun bindParameters(
        statement: PreparedStatement,
        entity: T,
    )

    protected abstract fun getLogger(): KLogger

    // Optional hooks for subclasses

    /**
     * Hook called after batch is executed
     * @param inserted Number of records inserted
     * @param batchSize Size of the batch
     */
    protected open fun onBatchExecuted(
        inserted: Int,
        batchSize: Int,
    ) {}

    protected open fun onFlushComplete() {
        getLogger().info { "Flushed all pending data" }
    }

    protected open fun onCloseComplete() {
        getLogger().info { "${this::class.simpleName} closed" }
    }
}
