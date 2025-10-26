package com.flo.nem12

import com.flo.nem12.command.NEM12ParseCommand
import com.flo.nem12.config.DatabaseConfig
import com.flo.nem12.exception.ParseException
import com.flo.nem12.handler.CompositeFailureHandler
import com.flo.nem12.handler.DatabaseFailureHandler
import com.flo.nem12.handler.LoggingFailureHandler
import com.flo.nem12.repository.impl.FailureReadingsRepositoryImpl
import com.flo.nem12.repository.impl.MeterReadingRepositoryImpl
import com.flo.nem12.service.NEM12ParserService
import com.flo.nem12.service.impl.NEM12ParserServiceImpl
import com.flo.nem12.service.impl.RecordParserServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Paths
import java.sql.DriverManager
import kotlin.io.path.Path
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

/**
 * Main entry point for NEM12 parser CLI
 * Acts as a Handler/Controller layer
 *
 * Architecture:
 * - Handler: Main.kt (this file)
 * - Service: NEM12ParserService
 * - Repository: MeterReadingRepository
 *
 * Usage:
 *   java -jar nem12-parser.jar <input-file> <output-db> [--batch-size=1000]
 */
fun main(args: Array<String>) {
    if (args.size < 2) {
        exitProcess(1)
    }
    val outputPath = Paths.get(args[1]) ?: Path("output.db")
    val connection = DriverManager.getConnection("jdbc:sqlite:$outputPath")

    try {
        val inputPath = Paths.get(args[0])

        // Parse options
        val batchSize =
            parseOption(args, "--batch-size")?.toInt()
                ?: DatabaseConfig.DEFAULT_BATCH_SIZE

        logger.info { "Starting NEM12 parser" }
        logger.info { "Input file: $inputPath" }
        logger.info { "Output database: $outputPath" }
        logger.info { "Batch size: $batchSize" }
        val cmd = NEM12ParseCommand(inputPath, outputPath)

        // Create repositories
        val meterRepository = MeterReadingRepositoryImpl(connection, batchSize)
        val failureRepository = FailureReadingsRepositoryImpl(connection, batchSize)

        // Create failure handlers
        val databaseHandler = DatabaseFailureHandler(failureRepository)
        val loggingHandler = LoggingFailureHandler()
        val failureHandler = CompositeFailureHandler(databaseHandler, loggingHandler)

        val recordParserService = RecordParserServiceImpl(failureHandler)

        // Create service with dependencies
        val service: NEM12ParserService = NEM12ParserServiceImpl(meterRepository, recordParserService)
        service.parseFile(cmd)

        // Print statistics
        val stats = failureHandler.getStatistics()
        logger.info { "Parsing completed successfully" }
        println("Database created: $outputPath")
        if (stats.isNotEmpty()) {
            println("Failed records:")
            stats.forEach { (reason, count) ->
                println("  $reason: $count")
            }
            println("Failed records database: $outputPath")
        }
    } catch (e: ParseException) {
        logger.error(e) { "Parse error: ${e.message}" }
        System.err.println("Parse error: ${e.message}")
        exitProcess(3)
    } catch (e: Exception) {
        logger.error(e) { "Unexpected error: ${e.message}" }
        System.err.println("Error: ${e.message}")
        exitProcess(2)
    } finally {
        connection.close()
    }
}

private fun parseOption(
    args: Array<String>,
    optionName: String,
): String? {
    return args.find { it.startsWith("$optionName=") }
        ?.substringAfter("=")
}
