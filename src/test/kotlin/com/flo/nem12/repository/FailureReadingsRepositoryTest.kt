package com.flo.nem12.repository

import com.flo.nem12.model.FailureReason
import com.flo.nem12.model.FailureRecord
import com.flo.nem12.repository.impl.FailureReadingsRepositoryImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.DriverManager
import java.time.LocalDateTime
import kotlin.io.path.deleteIfExists
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FailureReadingsRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    private val testDbPath: Path
        get() = tempDir.resolve("test.db")

    @AfterEach
    fun cleanup() {
        testDbPath.deleteIfExists()
    }

    @Test
    fun `should insert single failure record`() {
        // Given
        val dbPath = testDbPath
        val failure =
            FailureRecord(
                lineNumber = 42,
                nmi = "1234567890",
                intervalIndex = 10,
                rawValue = "-10.5",
                reason = FailureReason.NEGATIVE_VALUE,
                timestamp = LocalDateTime.of(2024, 1, 1, 12, 0),
            )
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")

        // When
        FailureReadingsRepositoryImpl(connection, 10).use { repository ->
            repository.save(failure)
        }

        // Then
        val resultSet =
            connection.createStatement()
                .executeQuery("SELECT COUNT(*) as cnt FROM failure_reading")
        assertEquals(1, resultSet.getInt("cnt"))
        connection.close()
    }

    @Test
    fun `should insert multiple failure records`() {
        // Given
        val dbPath = testDbPath
        val failures =
            listOf(
                FailureRecord(1, "NMI1", 0, "", FailureReason.EMPTY_VALUE, LocalDateTime.of(2024, 1, 1, 0, 0)),
                FailureRecord(2, "NMI2", 1, "ABC", FailureReason.NON_NUMERIC_VALUE, LocalDateTime.of(2024, 1, 1, 0, 30)),
                FailureRecord(3, "NMI3", 2, "-10.5", FailureReason.NEGATIVE_VALUE, LocalDateTime.of(2024, 1, 1, 1, 0)),
            )
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")

        // When
        FailureReadingsRepositoryImpl(connection, 10).use { repository ->
            failures.forEach { repository.save(it) }
        }

        // Then
        val resultSet =
            connection.createStatement()
                .executeQuery("SELECT COUNT(*) as cnt FROM failure_reading")
        assertEquals(3, resultSet.getInt("cnt"))
        connection.close()
    }

    @Test
    fun `should handle batch processing correctly`() {
        // Given
        val dbPath = testDbPath
        val batchSize = 5
        val failures =
            (0..11).map { i ->
                FailureRecord(
                    lineNumber = i,
                    nmi = "NMI_$i",
                    intervalIndex = i,
                    rawValue = "invalid_$i",
                    reason = FailureReason.EMPTY_VALUE,
                    timestamp = LocalDateTime.of(2024, 1, 1, 0, 0).plusMinutes(i * 30L),
                )
            }
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")

        // When
        // It will store 10 rows because flush method will not be called
        val repository = FailureReadingsRepositoryImpl(connection, batchSize)
        failures.forEach { repository.save(it) }

        // Then
        val resultSet =
            connection.createStatement()
                .executeQuery("SELECT COUNT(*) as cnt FROM failure_reading")
        assertEquals(10, resultSet.getInt("cnt"))
        connection.close()
    }

    @Test
    fun `should track statistics by failure reason`() {
        // Given
        val dbPath = testDbPath
        val failures =
            listOf(
                FailureRecord(1, "NMI1", 0, "", FailureReason.EMPTY_VALUE),
                FailureRecord(2, "NMI2", 0, "", FailureReason.EMPTY_VALUE),
                FailureRecord(3, "NMI3", 0, "ABC", FailureReason.NON_NUMERIC_VALUE),
                FailureRecord(4, "NMI4", 0, "-5", FailureReason.NEGATIVE_VALUE),
                FailureRecord(5, "NMI5", 0, "-10", FailureReason.NEGATIVE_VALUE),
                FailureRecord(6, "NMI6", 0, "-15", FailureReason.NEGATIVE_VALUE),
            )
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")

        // When
        val repository = FailureReadingsRepositoryImpl(connection, 10)
        failures.forEach { repository.save(it) }
        val stats = repository.getStatistics()
        repository.close()

        // Then
        assertEquals(2, stats[FailureReason.EMPTY_VALUE])
        assertEquals(1, stats[FailureReason.NON_NUMERIC_VALUE])
        assertEquals(3, stats[FailureReason.NEGATIVE_VALUE])
        connection.close()
    }

    @Test
    fun `should persist data with different failure reasons`() {
        // Given
        val dbPath = testDbPath
        val failures =
            listOf(
                FailureRecord(1, "NMI1", 0, "", FailureReason.EMPTY_VALUE),
                FailureRecord(2, "NMI2", 1, "ABC", FailureReason.NON_NUMERIC_VALUE),
                FailureRecord(3, "NMI3", 2, "-10.5", FailureReason.NEGATIVE_VALUE),
                FailureRecord(4, "NMI4", 3, "123.12345", FailureReason.INVALID_CONSUMPTION_FORMAT),
                FailureRecord(5, "NMI5", null, "2024-01-01", FailureReason.INVALID_DATE_FORMAT),
            )
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")

        // When
        FailureReadingsRepositoryImpl(connection, 10).use { repository ->
            failures.forEach { repository.save(it) }
        }

        // Then
        val resultSet =
            connection.createStatement()
                .executeQuery("SELECT DISTINCT failure_reason FROM failure_reading ORDER BY failure_reason")
        val reasons = mutableListOf<String>()
        while (resultSet.next()) {
            reasons.add(resultSet.getString("failure_reason"))
        }
        assertEquals(
            listOf(
                "EMPTY_VALUE",
                "INVALID_CONSUMPTION_FORMAT",
                "INVALID_DATE_FORMAT",
                "NEGATIVE_VALUE",
                "NON_NUMERIC_VALUE",
            ),
            reasons,
        )
        connection.close()
    }

    @Test
    fun `should handle null interval index`() {
        // Given
        val dbPath = testDbPath
        val failure =
            FailureRecord(
                lineNumber = 1,
                nmi = "NMI1",
                intervalIndex = null,
                rawValue = "2024-01-01",
                reason = FailureReason.INVALID_DATE_FORMAT,
            )
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")

        // When
        FailureReadingsRepositoryImpl(connection, 10).use { repository ->
            repository.save(failure)
        }

        // Then
        val resultSet =
            connection.createStatement()
                .executeQuery("SELECT interval_index FROM failure_reading")
        assertTrue(resultSet.next())
        assertEquals(0, resultSet.getInt("interval_index"))
        assertTrue(resultSet.wasNull()) // Check that it's actually NULL
        connection.close()
    }

    @Test
    fun `should handle null timestamp`() {
        // Given
        val dbPath = testDbPath
        val failure =
            FailureRecord(
                lineNumber = 1,
                nmi = "NMI1",
                intervalIndex = 0,
                rawValue = "",
                reason = FailureReason.EMPTY_VALUE,
                timestamp = null,
            )
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")

        // When
        FailureReadingsRepositoryImpl(connection, 10).use { repository ->
            repository.save(failure)
        }

        // Then
        val resultSet =
            connection.createStatement()
                .executeQuery("SELECT timestamp FROM failure_reading")
        assertTrue(resultSet.next())
        assertEquals(null, resultSet.getString("timestamp"))
        connection.close()
    }

    @Test
    fun `should flush remaining records on close`() {
        // Given
        val dbPath = testDbPath
        val batchSize = 10
        val failures =
            (0..4).map { i -> // Only 5 records, less than batch size
                FailureRecord(
                    lineNumber = i,
                    nmi = "NMI_$i",
                    intervalIndex = i,
                    rawValue = "invalid_$i",
                    reason = FailureReason.EMPTY_VALUE,
                )
            }
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")

        // When
        FailureReadingsRepositoryImpl(connection, batchSize).use { repository ->
            failures.forEach { repository.save(it) }
        } // close() will flush remaining records

        // Then
        val resultSet =
            connection.createStatement()
                .executeQuery("SELECT COUNT(*) as cnt FROM failure_reading")
        assertEquals(5, resultSet.getInt("cnt"))
        connection.close()
    }

    @Test
    fun `should insert failure record with is_processed set to false`() {
        // Given
        val dbPath = testDbPath
        val failure = FailureRecord(
            lineNumber = 1,
            nmi = "1234567890",
            intervalIndex = 0,
            rawValue = "invalid",
            reason = FailureReason.EMPTY_VALUE,
            timestamp = LocalDateTime.of(2024, 1, 1, 0, 0)
        )
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")

        // When
        FailureReadingsRepositoryImpl(connection, 10).use { repository ->
            repository.save(failure)
        }

        // Then
        val resultSet = connection.createStatement()
            .executeQuery("SELECT is_processed FROM failure_reading")
        assertTrue(resultSet.next())
        assertFalse(resultSet.getBoolean("is_processed"))
        connection.close()
    }
}
