package com.flo.nem12.generator

import com.flo.nem12.model.MeterReading
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedWriter
import java.nio.file.Path
import java.time.format.DateTimeFormatter
import kotlin.io.path.bufferedWriter

private val logger = KotlinLogging.logger {}

/**
 * PostgreSQL COPY command generator
 * Fastest bulk insert method for PostgreSQL
 */
class CopyCommandGenerator(outputPath: Path) : SQLGenerator {
    private val writer: BufferedWriter = outputPath.bufferedWriter()
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private var headerWritten = false

    override fun addReading(reading: MeterReading) {
        if (!headerWritten) {
            writeHeader()
            headerWritten = true
        }

        // Write CSV format data
        val line = "${reading.nmi},${reading.timestamp.format(timestampFormatter)},${reading.consumption}\n"
        writer.write(line)
    }

    override fun flush() {
        writer.flush()
    }

    override fun close() {
        if (headerWritten) {
            writeFooter()
        }
        writer.close()
        logger.info { "CopyCommandGenerator closed" }
    }

    private fun writeHeader() {
        writer.write("COPY meter_readings (nmi, timestamp, consumption) FROM STDIN WITH (FORMAT CSV);\n")
    }

    private fun writeFooter() {
        writer.write("\\.\n")
    }
}
