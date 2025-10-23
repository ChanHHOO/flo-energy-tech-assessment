package com.flo.nem12.parser

import com.flo.nem12.exception.ParseException
import com.flo.nem12.generator.SQLGenerator
import com.flo.nem12.model.RecordType
import com.flo.nem12.util.DateTimeValidator
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.bufferedReader

private val logger = KotlinLogging.logger {}

/**
 * Main NEM12 file parser using state machine pattern
 */
class NEM12Parser(private val sqlGenerator: SQLGenerator) {
    private val recordParser = RecordParser()
    private val state = ParserState()

    /**
     * Parse NEM12 file and generate SQL output
     *
     * @param filePath Path to NEM12 input file
     * @throws ParseException if parsing fails
     */
    fun parse(filePath: Path) {
        logger.info { "Starting to parse file: $filePath" }

        filePath.bufferedReader().use { reader ->
            reader.lineSequence().forEach { line ->
                state.incrementLineNumber()
                parseLine(line.trim())
            }
        }

        validateFileEnd()
        sqlGenerator.flush()

        logger.info { "Successfully parsed ${state.lineNumber} lines" }
    }

    private fun parseLine(line: String) {
        if (line.isEmpty()) return
        val recordType = RecordType.fromLine(line)

        when (recordType) {
            RecordType.HEADER -> handleHeader(line)
            RecordType.NMI_DATA -> handleNmiData(line)
            RecordType.INTERVAL_DATA -> handleIntervalData(line)
            RecordType.NMI_END -> handleNmiEnd()
            RecordType.FILE_END -> handleFileEnd()
        }
    }

    /**
     * Validates and processes 100 (Header) record
     *
     * Format: 100,VersionHeader,DateTime,FromParticipant,ToParticipant
     *
     * Validations:
     * 1. RecordIndicator must be 100
     * 2. VersionHeader must be "NEM12"
     * 3. DateTime must be valid 12-character format (YYYYMMDDHHmm)
     * 4. FromParticipant must be 1-10 characters
     * 5. ToParticipant must be 1-10 characters
     * 6. Must be exactly 5 fields
     * 7. Must be the first line in the file
     */
    private fun handleHeader(line: String) {
        // Validation 1: Must be first line
        if (state.lineNumber != 1) {
            throw ParseException(state.lineNumber, "Header (100) must be the first line")
        }

        val fields = line.split(",")

        // Validation 2: Must have exactly 5 fields
        if (fields.size != 5) {
            throw ParseException(
                state.lineNumber,
                "Header must have exactly 5 fields, found ${fields.size}"
            )
        }

        val recordIndicator = fields[0]
        val versionHeader = fields[1]
        val dateTime = fields[2]
        val fromParticipant = fields[3]
        val toParticipant = fields[4]

        // Validation 3: RecordIndicator must be 100
        if (recordIndicator != "100") {
            throw ParseException(
                state.lineNumber,
                "RecordIndicator must be '100', found '$recordIndicator'"
            )
        }

        // Validation 4: VersionHeader must be NEM12
        if (versionHeader != "NEM12") {
            throw ParseException(
                state.lineNumber,
                "VersionHeader must be 'NEM12', found '$versionHeader'"
            )
        }

        // Validation 5: DateTime must be valid 12-character format (YYYYMMDDHHmm)
        if (!DateTimeValidator.isValidISO8601DateTime(dateTime)) {
            throw ParseException(
                state.lineNumber,
                "DateTime must be 12 characters in YYYYMMDDHHmm format, found '$dateTime'"
            )
        }

        // Validation 6: FromParticipant must be 1-10 characters
        if (fromParticipant.isBlank() || fromParticipant.length > 10) {
            throw ParseException(
                state.lineNumber,
                "FromParticipant must be 1-10 characters, found '$fromParticipant' (${fromParticipant.length} chars)"
            )
        }

        // Validation 7: ToParticipant must be 1-10 characters
        if (toParticipant.isBlank() || toParticipant.length > 10) {
            throw ParseException(
                state.lineNumber,
                "ToParticipant must be 1-10 characters, found '$toParticipant' (${toParticipant.length} chars)"
            )
        }

        logger.info {
            "Valid header: version=$versionHeader, dateTime=$dateTime, " +
            "from=$fromParticipant, to=$toParticipant"
        }
    }

    private fun handleNmiData(line: String) {
        val fields = line.split(",")
        if (fields.size < 9) {
            throw ParseException(state.lineNumber, "Invalid 200 record: insufficient fields")
        }

        val nmi = fields[1]
        val intervalMinutes = fields[8].toInt()

        state.startNmiBlock(nmi, intervalMinutes)
        logger.info { "Started NMI block: $nmi with interval $intervalMinutes minutes" }
    }

    private fun handleIntervalData(line: String) {
        if (!state.insideNmiBlock) {
            throw ParseException(state.lineNumber, "300 record found outside NMI block")
        }

        val nmi = state.currentNmi ?: throw ParseException(state.lineNumber, "No current NMI")
        val readings = recordParser.parseIntervalData(line, nmi, state.intervalMinutes)

        readings.forEach { sqlGenerator.addReading(it) }

        logger.debug { "Generated ${readings.size} readings from interval data" }
    }

    private fun handleNmiEnd() {
        if (!state.insideNmiBlock) {
            throw ParseException(state.lineNumber, "500 record found outside NMI block")
        }

        logger.debug { "Ending NMI block at line ${state.lineNumber}" }
        state.endNmiBlock()
    }

    private fun handleFileEnd() {
        logger.info { "Reached end of file at line ${state.lineNumber}" }
    }

    private fun validateFileEnd() {
        if (state.insideNmiBlock) {
            throw ParseException(
                state.lineNumber,
                "File ended without closing NMI block (missing 500 record)"
            )
        }
    }
}
