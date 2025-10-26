package com.flo.nem12.model

/**
 * NEM12 file record type definitions
 */
enum class RecordType(val code: Int) {
    HEADER(100),
    NMI_DATA(200),
    INTERVAL_DATA(300),
    INTERVAL_EVENT(400),
    B2B_DETAIL(500),
    FILE_END(900),
    ;

    companion object {
        fun fromCode(code: Int): RecordType {
            return RecordType.entries.find { it.code == code }
                ?: throw IllegalArgumentException("Unknown record type: $code")
        }

        fun fromLine(line: String): RecordType {
            // All the RecordIndicators are following type Numeric(3)
            require(line.length >= 3) { "Invalid line format" }
            val code = line.take(3).toInt()
            return fromCode(code)
        }
    }
}
