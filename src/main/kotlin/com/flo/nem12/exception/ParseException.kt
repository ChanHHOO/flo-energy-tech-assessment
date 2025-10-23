package com.flo.nem12.exception

/**
 * Exception thrown during NEM12 file parsing
 */
class ParseException(
    val lineNumber: Int,
    message: String,
    cause: Throwable? = null
) : Exception("Line $lineNumber: $message", cause)
