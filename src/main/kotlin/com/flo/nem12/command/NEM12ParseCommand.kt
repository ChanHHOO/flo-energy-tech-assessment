package com.flo.nem12.command

import java.nio.file.Files
import java.nio.file.Path

data class NEM12ParseCommand (
    val batchSize: Int,
    val inputPath: Path,
    val outputPath: Path,
) {
    init {
        require(batchSize > 0) { "Batch size must be positive" }
        require(Files.exists(inputPath)) { "Input file does not exist" }
    }
}
