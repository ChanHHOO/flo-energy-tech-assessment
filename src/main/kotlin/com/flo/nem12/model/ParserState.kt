package com.flo.nem12.model

/**
 * Maintains parser state across NEM12 file processing
 */
data class ParserState(
    var currentNmi: String? = null,
    var intervalMinutes: Int = 0,
    var insideNmiBlock: Boolean = false,
    var lineNumber: Int = 0
) {
    fun startNmiBlock(nmi: String, interval: Int) {
        currentNmi = nmi
        intervalMinutes = interval
        insideNmiBlock = true
    }

    fun endNmiBlock() {
        currentNmi = null
        intervalMinutes = 0
        insideNmiBlock = false
    }

    fun incrementLineNumber() {
        lineNumber++
    }
}