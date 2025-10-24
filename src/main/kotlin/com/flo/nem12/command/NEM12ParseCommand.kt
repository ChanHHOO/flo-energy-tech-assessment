package com.flo.nem12.command

import java.nio.file.Path

data class NEM12ParseCommand (
    val batchSize: Int,
    val inputPath: Path,
    val outputPath: Path,
)