package com.flo.nem12

import com.flo.nem12.config.DatabaseConfig
import com.flo.nem12.exception.ParseException
import com.flo.nem12.generator.SQLiteGenerator
import com.flo.nem12.parser.NEM12Parser
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Paths
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

/**
 * Main entry point for NEM12 parser CLI
 *
 * Usage:
 *   java -jar nem12-parser.jar <input-file> <output-db> [--batch-size=1000]
 */
fun main(args: Array<String>) {
    if (args.size < 2) {
        exitProcess(1)
    }

    try {
        val inputPath = Paths.get(args[0])
        val outputPath = Paths.get(args[1])

        // Parse options
        val batchSize = parseOption(args, "--batch-size")?.toInt()
            ?: DatabaseConfig.DEFAULT_BATCH_SIZE

        logger.info { "Starting NEM12 parser" }
        logger.info { "Input file: $inputPath" }
        logger.info { "Output database: $outputPath" }
        logger.info { "Batch size: $batchSize" }

        // Create SQLite generator
        // It can be developed by Factory Pattern but now I considered only SQLite as DB Engine
        val generator = SQLiteGenerator(outputPath, batchSize)

        // Parse file
        generator.use { gen ->
            val parser = NEM12Parser(gen)
            parser.parse(inputPath)
        }

        logger.info { "Parsing completed successfully" }
        println("Database created: $outputPath")

    } catch (e: ParseException) {
        logger.error(e) { "Parse error: ${e.message}" }
        System.err.println("Parse error: ${e.message}")
        exitProcess(3)
    } catch (e: Exception) {
        logger.error(e) { "Unexpected error: ${e.message}" }
        System.err.println("Error: ${e.message}")
        exitProcess(2)
    }
}

private fun parseOption(args: Array<String>, optionName: String): String? {
    return args.find { it.startsWith("$optionName=") }
        ?.substringAfter("=")
}