package com.transit.arctransit.reconciliation.domain;

/**
 * Lifecycle state of an operational exception.
 *
 * Matches the CHECK constraint ck_exception_status in V11__create_operational_exceptions.sql.
 */
public enum ExceptionStatus {
    OPEN,
    ACKNOWLEDGED,
    INVESTIGATING,
    RESOLVED,
    DISMISSED
}
