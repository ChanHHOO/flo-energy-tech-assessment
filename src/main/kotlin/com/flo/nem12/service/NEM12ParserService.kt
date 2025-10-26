package com.flo.nem12.service

import com.flo.nem12.command.NEM12ParseCommand

/**
 * Service interface for parsing NEM12 files
 */
interface NEM12ParserService {
    /**
     * Parse NEM12 file and store readings in database
     *
     * @param inputPath Path to NEM12 input file
     * @throws com.flo.nem12.exception.ParseException if parsing fails
     */
    fun parseFile(cmd: NEM12ParseCommand)
}
