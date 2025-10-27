package com.flo.nem12.config

/**
 * Database configuration and schema definitions
 */
object DatabaseConfig {
    /**
     * Default batch size for batch inserts
     */
    const val DEFAULT_BATCH_SIZE = 50

    const val CREATE_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS meter_reading (
            id TEXT PRIMARY KEY,
            nmi VARCHAR(10) NOT NULL,
            timestamp TIMESTAMP NOT NULL,
            consumption NUMERIC NOT NULL,
            UNIQUE(nmi, timestamp)
        )
    """

    const val INSERT_SQL = """
        INSERT OR IGNORE INTO meter_reading (id, nmi, timestamp, consumption)
        VALUES (?, ?, ?, ?)
    """

    const val TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"

    const val CREATE_FAILED_READING_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS failure_reading (
            id TEXT PRIMARY KEY,
            line_number INTEGER NOT NULL,
            nmi TEXT,
            interval_index INTEGER,
            raw_value TEXT NOT NULL,
            failure_reason TEXT NOT NULL,
            timestamp TIMESTAMP
        )
    """

    const val INSERT_FAILED_READING_SQL = """
        INSERT INTO failure_reading (id, line_number, nmi, interval_index, raw_value, failure_reason, timestamp)
        VALUES (?, ?, ?, ?, ?, ?, ?)
    """
}
