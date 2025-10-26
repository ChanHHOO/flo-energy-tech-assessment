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
     * Value is not numeric when a number is expected
     */
    NON_NUMERIC_VALUE,

    /**
     * Negative consumption value (not allowed in NEM12)
     */
    NEGATIVE_VALUE,

    /**
     * Decimal scale exceeds 4 digits (15.4 format violation)
     */
    INVALID_DECIMAL_SCALE,

    /**
     * Integer digits exceed 15 digits (15.4 format violation)
     */
    INVALID_INTEGER_DIGITS,

    /**
     * Date format does not match expected format (yyyyMMdd)
     */
    INVALID_DATE_FORMAT,

    /**
     * Insufficient fields in record
     */
    INSUFFICIENT_FIELDS,

    /**
     * Unknown or unclassified error
     */
    UNKNOWN
}
