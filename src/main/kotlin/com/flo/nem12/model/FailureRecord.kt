package com.flo.nem12.model

import java.time.LocalDateTime

/**
 * Record representing a parsing failure
 *
 * @property lineNumber Line number where the failure occurred
 * @property nmi NMI identifier (null if failure occurred before NMI was established)
 * @property intervalIndex Interval index within the 300 record (null if not applicable)
 * @property rawValue The raw value that caused the failure
 * @property reason Classification of the failure
 * @property timestamp When the failure was recorded
 */
data class FailureRecord(
    val lineNumber: Int,
    val nmi: String?,
    val intervalIndex: Int?,
    val rawValue: String,
    val reason: FailureReason,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
