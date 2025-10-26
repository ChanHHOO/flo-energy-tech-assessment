package com.flo.nem12.model

/**
 * Enumeration of possible failure reasons during NEM12 parsing
 */
enum class FailureReason {
    /**
     * Empty or blank value where a valid value is expected
     */
    EMPTY_VALUE,

    /**
     * The number of fields is grater then 3
     * */
    INVALID_FIELDS,
    /**
     * Value is not numeric when a number is expected
     */
    NON_NUMERIC_VALUE,

    /**
     * Negative consumption value (not allowed in NEM12)
     */
    NEGATIVE_VALUE,

    /**
     * CONSUMPTION is follow 15.4
     */
    INVALID_CONSUMPTION_FORMAT,

    /**
     * Date format does not match expected format (yyyyMMdd)
     */
    INVALID_DATE_FORMAT,

    /**
     * Number of intervals does not match expected count
     */
    INTERVAL_COUNT_MISMATCH,

    /**
     * Unknown or unclassified error
     */
    UNKNOWN,
}
