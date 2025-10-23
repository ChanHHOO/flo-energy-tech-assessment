package com.flo.nem12.model

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Represents a single meter reading record
 * Corresponds to the meter_readings table in the database
 */
data class MeterReading(
    val nmi: String,
    val timestamp: LocalDateTime,
    val consumption: BigDecimal
)
