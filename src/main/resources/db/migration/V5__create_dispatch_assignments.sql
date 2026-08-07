/*
 * Table:
 *      dispatch_assignments
 *
 * NOTE:
 * - A dispatch assignment links one bus, one driver, and one route
 *   for a specific date and departure time.
 * - The same bus cannot be assigned twice at the same date and time.
 * - The same driver cannot be assigned twice at the same date and time.
 *   Both constraints are enforced by unique indexes below.
 *
 * - The Java application service performs additional validation:
 *   - The fleet unit must be ACTIVE (not INACTIVE or archived).
 *   - The driver must be ACTIVE and have a valid (non-expired) license.
 *   - The route must be ACTIVE (not INACTIVE or archived).
 *
 * - Assignments follow a lifecycle state machine:
 *   SCHEDULED   -> The trip is planned but has not started.
 *   IN_PROGRESS -> The bus has departed on the route.
 *   COMPLETED   -> The trip finished successfully.
 *   CANCELLED   -> The trip was cancelled before completion.
 *
 *   Valid transitions:
 *     SCHEDULED   -> IN_PROGRESS  (driver departs)
 *     SCHEDULED   -> CANCELLED    (assignment cancelled before departure)
 *     IN_PROGRESS -> COMPLETED    (trip finished)
 *     IN_PROGRESS -> CANCELLED    (trip aborted mid-route)
 *
 * - Foreign keys reference fleet_units, drivers, and routes.
 *   ON DELETE RESTRICT prevents physical deletion of any referenced
 *   record, ensuring dispatch history is always preserved.
 *
 * - Java/JPA manages the application timestamps.
 * - JPA optimistic locking uses the version column.
 */

CREATE TABLE arc.dispatch_assignments (

    /*
     * Internal database identifier for a dispatch assignment.
     */
    id BIGINT GENERATED ALWAYS AS IDENTITY,

    /*
     * The bus unit assigned to this dispatch.
     *
     * References arc.fleet_units(id).
     * RESTRICT prevents deleting a bus that has dispatch history.
     */
    fleet_unit_id BIGINT NOT NULL,

    /*
     * The driver assigned to operate the bus.
     *
     * References arc.drivers(id).
     * RESTRICT prevents deleting a driver that has dispatch history.
     */
    driver_id BIGINT NOT NULL,

    /*
     * The route the bus will follow.
     *
     * References arc.routes(id).
     * RESTRICT prevents deleting a route that has dispatch history.
     */
    route_id BIGINT NOT NULL,

    /*
     * The calendar date of the dispatched trip.
     *
     * Stored as DATE (no time component) because dispatch
     * scheduling is organized by calendar day.
     */
    dispatch_date DATE NOT NULL,

    /*
     * Planned departure time for this assignment.
     *
     * The scheduled_departure combined with dispatch_date determines
     * when the trip is expected to begin.
     */
    scheduled_departure TIMESTAMPTZ NOT NULL,

    /*
     * Planned arrival time for this assignment.
     * NULL means no estimated arrival was recorded.
     */
    scheduled_arrival TIMESTAMPTZ,

    /*
     * Actual departure time recorded by operations staff.
     * NULL until the trip starts (IN_PROGRESS).
     */
    actual_departure TIMESTAMPTZ,

    /*
     * Actual arrival time recorded by operations staff.
     * NULL until the trip completes (COMPLETED).
     */
    actual_arrival TIMESTAMPTZ,

    /*
     * Current lifecycle state of the dispatch assignment.
     *
     * A newly created assignment begins as SCHEDULED.
     */
    dispatch_status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',

    /*
     * Optional free-text notes from dispatch staff.
     * Ex: "Delayed due to traffic on EDSA"
     */
    notes TEXT,

    /*
     * Time when this assignment record was first created.
     */
    created_at TIMESTAMPTZ NOT NULL,

    /*
     * Time when this assignment record was last changed.
     */
    updated_at TIMESTAMPTZ NOT NULL,

    /*
     * JPA optimistic-lock version.
     */
    version BIGINT NOT NULL DEFAULT 0,

    /*
     * Primary-key constraint.
     */
    CONSTRAINT pk_dispatch_assignments
        PRIMARY KEY (id),

    /*
     * Foreign key: the assigned bus unit.
     */
    CONSTRAINT fk_dispatch_fleet_unit
        FOREIGN KEY (fleet_unit_id)
        REFERENCES arc.fleet_units (id)
        ON DELETE RESTRICT,

    /*
     * Foreign key: the assigned driver.
     */
    CONSTRAINT fk_dispatch_driver
        FOREIGN KEY (driver_id)
        REFERENCES arc.drivers (id)
        ON DELETE RESTRICT,

    /*
     * Foreign key: the assigned route.
     */
    CONSTRAINT fk_dispatch_route
        FOREIGN KEY (route_id)
        REFERENCES arc.routes (id)
        ON DELETE RESTRICT,

    /*
     * Dispatch status must be one of the accepted lifecycle states.
     */
    CONSTRAINT ck_dispatch_status
        CHECK (
            dispatch_status IN (
                'SCHEDULED',
                'IN_PROGRESS',
                'COMPLETED',
                'CANCELLED'
            )
        )
);

/*
 * Prevents double-booking a bus at the same date and departure time.
 *
 * Only non-cancelled assignments are checked because a cancelled
 * assignment should not block a replacement assignment.
 *
 * Implementation: partial unique index.
 */
CREATE UNIQUE INDEX ux_dispatch_fleet_unit_schedule
    ON arc.dispatch_assignments (fleet_unit_id, dispatch_date, scheduled_departure)
    WHERE dispatch_status <> 'CANCELLED';

/*
 * Prevents double-booking a driver at the same date and departure time.
 *
 * Same partial-index pattern as the bus constraint above.
 */
CREATE UNIQUE INDEX ux_dispatch_driver_schedule
    ON arc.dispatch_assignments (driver_id, dispatch_date, scheduled_departure)
    WHERE dispatch_status <> 'CANCELLED';
