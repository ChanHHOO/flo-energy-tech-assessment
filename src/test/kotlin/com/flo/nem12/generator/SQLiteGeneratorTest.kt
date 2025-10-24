package com.flo.nem12.generator

import com.flo.nem12.model.MeterReading
import com.flo.nem12.repository.SQLiteMeterReadingRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.math.BigDecimal
import java.nio.file.Path
import java.sql.DriverManager
import java.time.LocalDateTime
import kotlin.io.path.deleteIfExists
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SQLiteGeneratorTest {

    @TempDir
    lateinit var tempDir: Path

    private val testDbPath: Path
        get() = tempDir.resolve("test.db")

    @AfterEach
    fun cleanup() {
        testDbPath.deleteIfExists()
    }

    @Test
    fun `should insert single reading`() {
        // Given
        val dbPath = testDbPath
        val reading = MeterReading(
            nmi = "1234567890",
            timestamp = LocalDateTime.of(2024, 1, 1, 0, 0),
            consumption = BigDecimal("10.5")
        )

        // When
        SQLiteMeterReadingRepository(dbPath, 10).use { repository ->
            repository.save(reading)
        }

        // Then
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        val resultSet = connection.createStatement()
            .executeQuery("SELECT COUNT(*) as cnt FROM meter_readings")
        assertEquals(1, resultSet.getInt("cnt"))
        connection.close()
    }

    @Test
    fun `should insert multiple readings`() {
        // Given
        val dbPath = testDbPath
        val readings = listOf(
            MeterReading("1234567890", LocalDateTime.of(2024, 1, 1, 0, 0), BigDecimal("10.5")),
            MeterReading("1234567890", LocalDateTime.of(2024, 1, 1, 0, 30), BigDecimal("11.2")),
            MeterReading("9876543210", LocalDateTime.of(2024, 1, 1, 1, 0), BigDecimal("15.7"))
        )

        // When
        SQLiteMeterReadingRepository(dbPath, 10).use { repository ->
            readings.forEach { repository.save(it) }
        }

        // Then
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        val resultSet = connection.createStatement()
            .executeQuery("SELECT COUNT(*) as cnt FROM meter_readings")
        assertEquals(3, resultSet.getInt("cnt"))
        connection.close()
    }

    @Test
    fun `should handle batch processing correctly`() {
        // Given
        val dbPath = testDbPath
        val batchSize = 5
        val readings = (0..11).map { i ->
            MeterReading(
                nmi = "1234567890",
                timestamp = LocalDateTime.of(2024, 1, 1, 0, 0, 0).plusMinutes(i * 30L),
                consumption = BigDecimal("10.$i")
            )
        }

        // When
        // It will be store 10 rows because flush method will be not called.
        val repository = SQLiteMeterReadingRepository(dbPath, batchSize)
        readings.forEach { repository.save(it) }

        // Then
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        val resultSet = connection.createStatement()
            .executeQuery("SELECT COUNT(*) as cnt FROM meter_readings")
        assertEquals(10, resultSet.getInt("cnt"))
        connection.close()
    }

    @Test
    fun `should ignore duplicate readings with same nmi and timestamp`() {
        // Given
        val dbPath = testDbPath
        val reading1 = MeterReading(
            nmi = "1234567890",
            timestamp = LocalDateTime.of(2024, 1, 1, 0, 0),
            consumption = BigDecimal("10.5")
        )
        val reading2 = MeterReading(
            nmi = "1234567890",
            timestamp = LocalDateTime.of(2024, 1, 1, 0, 0),
            consumption = BigDecimal("99.9")
        )

        // When
        SQLiteMeterReadingRepository(dbPath, 10).use { repository ->
            repository.save(reading1)
            repository.save(reading2)
        }

        // Then
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        val resultSet = connection.createStatement()
            .executeQuery("SELECT COUNT(*) as cnt FROM meter_readings")
        assertEquals(1, resultSet.getInt("cnt"))

        val dataResult = connection.createStatement()
            .executeQuery("SELECT consumption FROM meter_readings")
        assertEquals("10.5", dataResult.getBigDecimal("consumption").toPlainString())
        connection.close()
    }

    @Test
    fun `should persist data with different NMIs`() {
        // Given
        val dbPath = testDbPath
        val readings = listOf(
            MeterReading("NMI_001", LocalDateTime.of(2024, 1, 1, 0, 0), BigDecimal("10.5")),
            MeterReading("NMI_002", LocalDateTime.of(2024, 1, 1, 0, 0), BigDecimal("20.3")),
            MeterReading("NMI_003", LocalDateTime.of(2024, 1, 1, 0, 0), BigDecimal("30.7"))
        )

        // When
        SQLiteMeterReadingRepository(dbPath, 10).use { repository ->
            readings.forEach { repository.save(it) }
        }

        // Then
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        val resultSet = connection.createStatement()
            .executeQuery("SELECT DISTINCT nmi FROM meter_readings ORDER BY nmi")
        val nmis = mutableListOf<String>()
        while (resultSet.next()) {
            nmis.add(resultSet.getString("nmi"))
        }
        assertEquals(listOf("NMI_001", "NMI_002", "NMI_003"), nmis)
        connection.close()
    }
}
