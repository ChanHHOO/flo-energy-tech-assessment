package com.flo.nem12.handler

import com.flo.nem12.model.FailureReason
import com.flo.nem12.model.FailureRecord
import com.flo.nem12.repository.FailureReadingsRepository

/**
 * Failure handler that persists failures to SQLite database
 * Uses batch processing for optimal performance
 */
class DatabaseFailureHandler(
    private val repository: FailureReadingsRepository
) : FailureHandler {
    override fun handleFailure(failure: FailureRecord) {
        repository.save(failure)
    }

    override fun getStatistics(): Map<FailureReason, Int> {
        return repository.getStatistics()
    }
}
