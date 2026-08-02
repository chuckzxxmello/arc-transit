package com.transit.arctransit.dispatch.domain;

/**
 * Lifecycle state of a dispatch assignment.
 *
 * Matches the database check constraint ck_dispatch_status
 * in V5__create_dispatch_assignments.sql.
 *
 * Valid transitions:
 *   SCHEDULED   -> IN_PROGRESS  (driver departs)
 *   SCHEDULED   -> CANCELLED    (assignment cancelled before departure)
 *   IN_PROGRESS -> COMPLETED    (trip finished)
 *   IN_PROGRESS -> CANCELLED    (trip aborted mid-route)
 */
public enum DispatchStatus {

    /** The trip is planned but has not started. */
    SCHEDULED,

    /** The bus has departed on the route. */
    IN_PROGRESS,

    /** The trip finished successfully. */
    COMPLETED,

    /** The trip was cancelled before or during execution. */
    CANCELLED
}
