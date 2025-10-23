package com.flo.nem12.generator

import com.flo.nem12.model.MeterReading
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedWriter
import java.nio.file.Path
import java.time.format.DateTimeFormatter
import kotlin.io.path.bufferedWriter

private val logger = KotlinLogging.logger {}

/**
 * Batch INSERT statement generator
 * Standard SQL approach for compatibility
 */
class BatchInsertGenerator(
    outputPath: Path,
    private val batchSize: Int = 1000
) : SQLGenerator {
    private val writer: BufferedWriter = outputPath.bufferedWriter()
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val buffer = mutableListOf<MeterReading>()

    override fun addReading(reading: MeterReading) {
        buffer.add(reading)

        if (buffer.size >= batchSize) {
            flush()
        }
    }

    override fun flush() {
        if (buffer.isEmpty()) return

        val sql = buildString {
            append("INSERT INTO meter_readings (nmi, timestamp, consumption) VALUES\n")

            buffer.forEachIndexed { index, reading ->
                append("('${reading.nmi}', '${reading.timestamp.format(timestampFormatter)}', ${reading.consumption})")
                if (index < buffer.size - 1) {
                    append(",\n")
                }
            }

            append(";\n\n")
        }

        writer.write(sql)
        logger.debug { "Flushed ${buffer.size} readings to SQL" }
        buffer.clear()
    }

    override fun close() {
        flush()
        writer.close()
        logger.info { "BatchInsertGenerator closed" }
    }
}
