package com.flo.nem12.service.impl

import com.flo.nem12.command.NEM12ParseCommand
import com.flo.nem12.exception.ParseException
import com.flo.nem12.model.MeterReading
import com.flo.nem12.repository.MeterReadingRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NEM12ParserServiceImplTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `should parse valid NEM12 file successfully`() {
        // Given
        val inputFile = tempDir.resolve("valid.nem12")
        val validContent = """
            100,NEM12,202401011200,SENDER,RECEIVER
            200,1234567890,E1,1,E1,N1,01009,KWH,30,20240101
            300,20240101,10.5,11.2,12.3,13.4,14.5,15.6,16.7,17.8,18.9,19.0,20.1,21.2,22.3,23.4,24.5,25.6,26.7,27.8,28.9,29.0,30.1,31.2,32.3,33.4,34.5,35.6,36.7,37.8,38.9,39.0,40.1,41.2,42.3,43.4,44.5,45.6,46.7,47.8,48.9,49.0,50.1,51.2,52.3,53.4,54.5,55.6,56.7,57.8,A,,,20050301120000,
            500,O,S01009,20240101120000,
            900
        """.trimIndent()
        Files.writeString(inputFile, validContent)

        val repository = TestMeterReadingRepository()
        val service = NEM12ParserServiceImpl(repository)
        val cmd = NEM12ParseCommand(50, inputFile, tempDir.resolve("output.db"))

        // When
        service.parseFile(cmd)

        // Then
        assertEquals(48, repository.savedReadings.size)
        assertTrue(repository.flushed)
        assertTrue(repository.closed)
    }

    @Test
    fun `should throw exception when header is not first line`() {
        // Given
        val inputFile = tempDir.resolve("invalid_header.nem12")
        val invalidContent = """
            200,1234567890,E1,1,E1,N1,01009,KWH,30,20240101
            100,NEM12,202401011200,SENDER,RECEIVER
        """.trimIndent()
        Files.writeString(inputFile, invalidContent)

        val repository = TestMeterReadingRepository()
        val service = NEM12ParserServiceImpl(repository)
        val cmd = NEM12ParseCommand(50, inputFile, tempDir.resolve("output.db"))

        // When & Then
        assertThrows<ParseException> {
            service.parseFile(cmd)
        }
    }

    @Test
    fun `should throw exception when NMI block is not closed`() {
        // Given
        val inputFile = tempDir.resolve("unclosed_nmi.nem12")
        val invalidContent = """
            100,NEM12,202401011200,SENDER,RECEIVER
            200,1234567890,E1,1,E1,N1,01009,KWH,30,20240101
            300,20240101,10.5,11.2,12.3,13.4,14.5,15.6,16.7,17.8,18.9,19.0,20.1,21.2,22.3,23.4,24.5,25.6,26.7,27.8,28.9,29.0,30.1,31.2,32.3,33.4,34.5,35.6,36.7,37.8,38.9,39.0,40.1,41.2,42.3,43.4,44.5,45.6,46.7,47.8,48.9,49.0,50.1,51.2,52.3,53.4,54.5,55.6,56.7,57.8,A,,,20050301120000,
            900
        """.trimIndent()
        Files.writeString(inputFile, invalidContent)

        val repository = TestMeterReadingRepository()
        val service = NEM12ParserServiceImpl(repository)
        val cmd = NEM12ParseCommand(50, inputFile, tempDir.resolve("output.db"))

        // When & Then
        assertThrows<ParseException> {
            service.parseFile(cmd)
        }
    }

    @Test
    fun `should throw exception when 300 record appears outside NMI block`() {
        // Given
        val inputFile = tempDir.resolve("300_outside_block.nem12")
        val invalidContent = """
            100,NEM12,202401011200,SENDER,RECEIVER
            300,20240101,10.5,11.2,12.3,13.4,14.5,15.6,16.7,17.8,18.9,19.0,20.1,21.2,22.3,23.4,24.5,25.6,26.7,27.8,28.9,29.0,30.1,31.2,32.3,33.4,34.5,35.6,36.7,37.8,38.9,39.0,40.1,41.2,42.3,43.4,44.5,45.6,46.7,47.8,48.9,49.0,50.1,51.2,52.3,53.4,54.5,55.6,56.7,57.8,A,,,20050301120000,
            900
        """.trimIndent()
        Files.writeString(inputFile, invalidContent)

        val repository = TestMeterReadingRepository()
        val service = NEM12ParserServiceImpl(repository)
        val cmd = NEM12ParseCommand(50, inputFile, tempDir.resolve("output.db"))

        // When & Then
        assertThrows<ParseException> {
            service.parseFile(cmd)
        }
    }

    /**
     * Mock for MeterReadingRepository
     */
    private class TestMeterReadingRepository : MeterReadingRepository {
        val savedReadings = mutableListOf<MeterReading>()
        var flushed = false
        var closed = false

        override fun save(reading: MeterReading) {
            savedReadings.add(reading)
        }

        override fun flush() {
            flushed = true
        }

        override fun close() {
            closed = true
        }
    }
}
