package com.flo.nem12.repository.impl

import com.flo.nem12.config.DatabaseConfig
import com.flo.nem12.model.FailureReason
import com.flo.nem12.model.FailureRecord
import com.flo.nem12.repository.BaseSQLiteRepository
import com.flo.nem12.repository.FailureReadingsRepository
import com.flo.nem12.util.DateTimeUtil.Companion.aestToUtc
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.sql.Connection
import java.sql.PreparedStatement
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Implementation of FailureReadingsRepository
 * Uses batch processing for optimal performance
 * Converts AEST timestamps to UTC before saving
 */
class FailureReadingsRepositoryImpl(
    connection: Connection,
    batchSize: Int = DatabaseConfig.DEFAULT_BATCH_SIZE,
) : BaseSQLiteRepository<FailureRecord>(connection, batchSize), FailureReadingsRepository {
    private val statistics = mutableMapOf<FailureReason, Int>()

    override fun getCreateTableSql(): String = DatabaseConfig.CREATE_FAILED_READING_TABLE_SQL

    override fun getInsertSql(): String = DatabaseConfig.INSERT_FAILED_READING_SQL

    override fun bindParameters(
        statement: PreparedStatement,
        entity: FailureRecord,
    ) {
        // Generate UUID for primary key
        val id = UUID.randomUUID().toString()

        // Set prepared statement parameters
        statement.setString(1, id)
        statement.setInt(2, entity.lineNumber)
        statement.setString(3, entity.nmi)
        if (entity.intervalIndex != null) {
            statement.setInt(4, entity.intervalIndex)
        } else {
            statement.setNull(4, java.sql.Types.INTEGER)
        }
        statement.setString(5, entity.rawValue)
        statement.setString(6, entity.reason.name)

        // Convert AEST to UTC before saving (if timestamp is not null)
        val utcTimestamp = entity.timestamp?.let { aestToUtc(it) }
        statement.setString(7, utcTimestamp?.format(timestampFormatter))

        // Update statistics
        statistics[entity.reason] = statistics.getOrDefault(entity.reason, 0) + 1
    }

    override fun getLogger(): KLogger = logger

    override fun getStatistics(): Map<FailureReason, Int> {
        return statistics.toMap()
    }
}
