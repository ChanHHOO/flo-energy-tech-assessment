package com.flo.nem12.repository.impl

import com.flo.nem12.config.DatabaseConfig
import com.flo.nem12.model.MeterReading
import com.flo.nem12.repository.BaseSQLiteRepository
import com.flo.nem12.repository.MeterReadingRepository
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.sql.Connection
import java.sql.PreparedStatement
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Implementation of MeterReadingRepository
 * Uses batch processing for optimal performance
 * Converts AEST timestamps to UTC before saving
 */
class MeterReadingRepositoryImpl(
    connection: Connection,
    batchSize: Int = DatabaseConfig.DEFAULT_BATCH_SIZE,
) : BaseSQLiteRepository<MeterReading>(connection, batchSize), MeterReadingRepository {
    private var totalInserted = 0

    override fun getCreateTableSql(): String = DatabaseConfig.CREATE_TABLE_SQL

    override fun getInsertSql(): String = DatabaseConfig.INSERT_SQL

    override fun bindParameters(
        statement: PreparedStatement,
        entity: MeterReading,
    ) {
        // Generate UUID for primary key
        val id = UUID.randomUUID().toString()

        // Convert AEST to UTC before saving
        val utcTimestamp = aestToUtc(entity.timestamp)

        // Set prepared statement parameters
        statement.setString(1, id)
        statement.setString(2, entity.nmi)
        statement.setString(3, utcTimestamp.format(timestampFormatter))
        statement.setString(4, entity.consumption.toPlainString())
    }

    override fun getLogger(): KLogger = logger

    override fun onBatchExecuted(
        inserted: Int,
        batchSize: Int,
    ) {
        totalInserted += inserted
    }
}
