package com.flo.nem12.integration

import com.flo.nem12.command.NEM12ParseCommand
import com.flo.nem12.config.DatabaseConfig
import com.flo.nem12.handler.DatabaseFailureHandler
import com.flo.nem12.repository.impl.FailureReadingsRepositoryImpl
import com.flo.nem12.repository.impl.MeterReadingRepositoryImpl
import com.flo.nem12.service.impl.NEM12ParserServiceImpl
import com.flo.nem12.service.impl.RecordParserServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.pathString

/**
 * Integration test that validates NEM12 CSV files from examples directory
 * Logs results in format: "${filename} : Success" or "${filename} : Fail, Because ${error}"
 */
class NEM12FileValidationTest {
    private val logger = KotlinLogging.logger {}

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `validate all NEM12 example files`() {
        val examplesDir = File("src/test/kotlin/com/flo/nem12/integration/samples")

        if (!examplesDir.exists() || !examplesDir.isDirectory) {
            logger.warn { "Examples directory not found: ${examplesDir.absolutePath}" }
            return
        }

        val csvFiles: Array<File> =
            examplesDir.listFiles { file: File ->
                file.isFile && file.extension.equals("csv", ignoreCase = true)
            }?.sortedBy { it.name }?.toTypedArray() ?: emptyArray()

        logger.info { "Found ${csvFiles.size} CSV files to validate" }
        logger.info { "=" * 80 }

        csvFiles.forEach { file: File ->
            validateFile(file)
        }

        logger.info { "=" * 80 }
        logger.info { "Validation completed for ${csvFiles.size} files" }
    }

    private fun validateFile(file: File) {
        val filename = file.name
        val outputDbPath = tempDir.resolve("$filename.db")

        var connection: Connection? = null

        try {
            // Create database connection
            connection = DriverManager.getConnection("jdbc:sqlite:${outputDbPath.pathString}")

            // Create repositories
            val meterRepository = MeterReadingRepositoryImpl(connection, DatabaseConfig.DEFAULT_BATCH_SIZE)
            val failureRepository = FailureReadingsRepositoryImpl(connection, DatabaseConfig.DEFAULT_BATCH_SIZE)

            // Create failure handler and parser service
            val failureHandler = DatabaseFailureHandler(failureRepository)
            val recordParserService = RecordParserServiceImpl(failureHandler)

            // Create parser service
            val parserService = NEM12ParserServiceImpl(meterRepository, recordParserService)

            // Create command
            val cmd = NEM12ParseCommand(file.toPath(), outputDbPath)

            // Attempt to parse the file
            parserService.parseFile(cmd)

            // If we get here, parsing succeeded
            logger.info { "$filename : Success" }
        } catch (e: Exception) {
            // Parsing failed
            val errorMessage = e.message ?: e.javaClass.simpleName
            logger.error { "$filename : Fail, Because $errorMessage" }
        } finally {
            // Clean up connection
            connection?.close()
        }
    }

    private operator fun String.times(count: Int): String = this.repeat(count)
}
