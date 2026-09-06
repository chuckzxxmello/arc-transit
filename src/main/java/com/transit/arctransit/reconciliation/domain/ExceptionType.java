package com.transit.arctransit.reconciliation.domain;

/**
 * Classification of operational exceptions detected by the reconciliation engine.
 *
 * Matches the CHECK constraint ck_exception_type in V11__create_operational_exceptions.sql.
 */
public enum ExceptionType {
    /** Conflicting states across modules (e.g., ACTIVE fleet unit assigned to IN_PROGRESS maintenance). */
    CONTRADICTION,

    /** Entity stuck in a state beyond configured threshold (e.g., dispatch IN_PROGRESS for 24h). */
    STALE_STATE,

    /** Incomplete records required for operations. */
    MISSING_DATA,

    /** Resource double-booking or overlap. */
    SCHEDULING_CONFLICT
}
