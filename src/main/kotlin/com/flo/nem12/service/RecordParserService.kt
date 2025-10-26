package com.flo.nem12.service

import com.flo.nem12.model.MeterReading

interface RecordParserService {
    /**
     * Parse 300 (interval data) record into list of MeterReading objects
     *
     * @param line Raw 300 record line
     * @param nmi Current NMI identifier
     * @param intervalMinutes Interval duration in minutes
     * @return List of parsed meter readings
     */
    fun parseIntervalData(line: String, nmi: String, intervalMinutes: Int): List<MeterReading>

    /**
     * Set line number for tracking issued line
     *
     * @param lineNumber
     */
    fun setLineNumber(lineNumber: Int)
}