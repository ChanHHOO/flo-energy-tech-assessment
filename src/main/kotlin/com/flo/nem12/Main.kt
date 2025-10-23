package com.flo.nem12

import com.flo.nem12.exception.ParseException
import com.flo.nem12.generator.BatchInsertGenerator
import com.flo.nem12.generator.CopyCommandGenerator
import com.flo.nem12.generator.SQLGenerator
import com.flo.nem12.parser.NEM12Parser
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Paths
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

/**
 * Main entry point for NEM12 parser CLI
 *
 * Usage:
 *   java -jar nem12-parser.jar <input-file> <output-file> [--mode=batch|copy] [--batch-size=1000]
 */
fun main(args: Array<String>) {
    if (args.size < 2) {
        printUsage()
        exitProcess(1)
    }

    try {
        val inputPath = Paths.get(args[0])
        val outputPath = Paths.get(args[1])

        // Parse options
        val mode = parseOption(args, "--mode") ?: "copy"
        val batchSize = parseOption(args, "--batch-size")?.toInt() ?: 1000

        logger.info { "Starting NEM12 parser" }
        logger.info { "Input file: $inputPath" }
        logger.info { "Output file: $outputPath" }
        logger.info { "Mode: $mode" }
        logger.info { "Batch size: $batchSize" }

        // Create SQL generator
        val generator = createGenerator(mode, outputPath, batchSize)

        // Parse file
        generator.use { gen ->
            val parser = NEM12Parser(gen)
            parser.parse(inputPath)
        }

        logger.info { "Parsing completed successfully" }
        println("SQL file generated: $outputPath")

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

private fun createGenerator(mode: String, outputPath: java.nio.file.Path, batchSize: Int): SQLGenerator {
    return when (mode.lowercase()) {
        "batch" -> BatchInsertGenerator(outputPath, batchSize)
        "copy" -> CopyCommandGenerator(outputPath)
        else -> throw IllegalArgumentException("Unknown mode: $mode")
    }
}

private fun parseOption(args: Array<String>, optionName: String): String? {
    return args.find { it.startsWith("$optionName=") }
        ?.substringAfter("=")
}

private fun printUsage() {
    println("""
        Usage: java -jar nem12-parser.jar <input-file> <output-file> [options]

        Options:
          --mode=batch|copy     SQL generation mode (default: copy)
          --batch-size=N        Batch size for batch mode (default: 1000)

        Examples:
          java -jar nem12-parser.jar input.nem12 output.sql
          java -jar nem12-parser.jar input.nem12 output.sql --mode=batch --batch-size=500
    """.trimIndent())
}
