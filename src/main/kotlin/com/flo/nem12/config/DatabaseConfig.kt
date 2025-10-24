package com.flo.nem12.config

/**
 * Database configuration and schema definitions
 */
object DatabaseConfig {

    /**
     * Default batch size for batch inserts
     */
    const val DEFAULT_BATCH_SIZE = 50

    /**
     * SQLite meter_readings table schema
     */
    const val CREATE_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS meter_readings (
            id TEXT PRIMARY KEY,
            nmi VARCHAR(10) NOT NULL,
            timestamp TIMESTAMP NOT NULL,
            consumption NUMERIC NOT NULL,
            UNIQUE(nmi, timestamp)
        )
    """

    /**
     * Insert statement with conflict handling (IGNORE duplicates)
     */
    const val INSERT_SQL = """
        INSERT OR IGNORE INTO meter_readings (id, nmi, timestamp, consumption)
        VALUES (?, ?, ?, ?)
    """

    /**
     * Timestamp format used in database
     */
    const val TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"
}