package com.flo.nem12.command

import java.nio.file.Files
import java.nio.file.Path

data class NEM12ParseCommand (
    val inputPath: Path,
    val outputPath: Path,
) {
    init {
        require(Files.exists(inputPath)) { "Input file does not exist" }
    }
}
