package com.flo.nem12.generator

import java.nio.file.Path

/**
 * Database type enum for generator factory
 */
enum class DatabaseType {
    SQLITE,
    // Future support: POSTGRESQL, MYSQL, etc.
}

/**
 * Factory for creating SQL generator instances
 */
object GeneratorFactory {
    /**
     * Creates a SQLGenerator instance based on database type
     *
     * @param type Database type to create generator for
     * @param dbPath Path to the database file or connection string
     * @param batchSize Batch size for bulk operations
     * @return SQLGenerator instance
     */
    fun createGenerator(
        type: DatabaseType,
        dbPath: Path,
        batchSize: Int
    ): SQLGenerator {
        return when (type) {
            DatabaseType.SQLITE -> SQLiteGenerator(dbPath, batchSize)
            // Future implementations:
            // DatabaseType.POSTGRESQL -> PostgreSQLGenerator(dbPath, batchSize)
            // DatabaseType.MYSQL -> MySQLGenerator(dbPath, batchSize)
        }
    }

    /**
     * Creates a SQLite generator (default)
     */
    fun createSQLiteGenerator(dbPath: Path, batchSize: Int): SQLGenerator {
        return createGenerator(DatabaseType.SQLITE, dbPath, batchSize)
    }
}