/*
 * Tables:
 *      routes
 *      route_stops
 *
 * Important rules:
 * - A route defines a fixed path that a bus follows repeatedly.
 * - Each route has an ordered list of stops (child table: route_stops).
 * - Route codes remain unique across active and archived records.
 * - The stop_sequence column determines the order of stops on a route.
 * - A route cannot have two stops with the same sequence number.
 * - A route cannot have two stops with the same name.
 * - Java/JPA manages the application timestamps.
 * - Route records are archived (soft-delete) instead of hard-deleted.
 * - JPA optimistic locking uses the version column.
 *
 * Route lifecycle:
 *   ACTIVE   -> The route is available for dispatch assignments.
 *   INACTIVE -> The route exists but is not currently operated.
 */

CREATE TABLE arc.routes (

    /*
     * Internal database identifier for a route.
     *
     * PostgreSQL generates this value automatically.
     * Application code does not choose the ID.
     */
    id BIGINT GENERATED ALWAYS AS IDENTITY,

    /*
     * Unique route identifier used by dispatch staff.
     *
     * Ex:
     *      RT-001
     *      RT-002
     *
     * This value must remain unique across active and archived records.
     */
    route_code VARCHAR(30) NOT NULL,

    /*
     * Human-readable name describing the route.
     *
     * Ex:
     *      "Quezon Avenue - EDSA Loop"
     *      "Commonwealth - SM North"
     */
    route_name VARCHAR(150) NOT NULL,

    /*
     * Optional longer description of the route purpose or coverage area.
     */
    description VARCHAR(500),

    /*
     * Current operational status of the route.
     *
     * A newly created route begins as ACTIVE so it is immediately
     * available for dispatch assignment.
     */
    route_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    /*
     * Estimated total duration of the route in minutes.
     *
     * This is an administrative estimate used for scheduling.
     * The actual trip duration depends on real-time conditions.
     */
    estimated_duration_minutes INT,

    /*
     * Time when the route record was first created.
     */
    created_at TIMESTAMPTZ NOT NULL,

    /*
     * Time when the route record was last changed.
     */
    updated_at TIMESTAMPTZ NOT NULL,

    /*
     * Soft-delete/archive timestamp.
     *
     * if archived_at is NULL:
     *      -> The route is active (not archived).
     *
     * if archived_at is NOT NULL:
     *      -> The route has been archived but remains stored
     *         for dispatch history references.
     */
    archived_at TIMESTAMPTZ,

    /*
     * JPA optimistic-lock version.
     */
    version BIGINT NOT NULL DEFAULT 0,

    /*
     * Primary-key constraint.
     */
    CONSTRAINT pk_routes
        PRIMARY KEY (id),

    /*
     * Prevents two route rows from using the same route code.
     * This constraint applies to every row, including archived rows.
     */
    CONSTRAINT ux_routes_route_code
        UNIQUE (route_code),

    /*
     * Route codes must be uppercase, trimmed, and nonblank.
     *
     * The Java service should normalize input before saving.
     * These checks provide the final database safety boundary.
     */
    CONSTRAINT ck_routes_route_code_normalized
        CHECK (route_code = BTRIM(UPPER(route_code))),

    CONSTRAINT ck_routes_route_code_not_blank
        CHECK (BTRIM(route_code) <> ''),

    /*
     * Route name must not be blank.
     */
    CONSTRAINT ck_routes_route_name_not_blank
        CHECK (BTRIM(route_name) <> ''),

    /*
     * Route status must be one of the accepted lifecycle states.
     */
    CONSTRAINT ck_routes_route_status
        CHECK (
            route_status IN (
                'ACTIVE',
                'INACTIVE'
            )
        ),

    /*
     * Estimated duration must be positive if provided.
     * NULL means no estimate has been recorded yet.
     */
    CONSTRAINT ck_routes_estimated_duration_positive
        CHECK (
            estimated_duration_minutes IS NULL
            OR estimated_duration_minutes > 0
        )
);


/*
 * Ordered stops belonging to a specific route.
 *
 * Each row represents one physical stop along a route.
 * The stop_sequence column determines the visiting order:
 *
 *   stop_sequence = 1  ->  first stop (origin terminal)
 *   stop_sequence = 2  ->  second stop
 *   ...
 *   stop_sequence = N  ->  last stop (destination terminal)
 *
 * Implementation pattern:
 *   The route_stops table is a child of routes. JPA maps this
 *   as a @OneToMany relationship with cascade and orphanRemoval,
 *   so deleting stops from the Java collection automatically
 *   removes the corresponding database rows.
 *
 *   Source: https://docs.oracle.com/en/java/javase/21/docs/api/java.persistence/
 *   (Ctrl+F: orphanRemoval)
 */
CREATE TABLE arc.route_stops (

    /*
     * Internal database identifier for a route stop.
     */
    id BIGINT GENERATED ALWAYS AS IDENTITY,

    /*
     * The parent route that this stop belongs to.
     *
     * ON DELETE CASCADE means that if the parent route row is
     * physically deleted (which should only happen in test or
     * migration scenarios, since normal removal uses archival),
     * all of its stops are automatically removed.
     */
    route_id BIGINT NOT NULL,

    /*
     * Human-readable name of the stop.
     *
     * Ex:
     *      "Quezon Avenue Station"
     *      "SM North EDSA Terminal"
     */
    stop_name VARCHAR(150) NOT NULL,

    /*
     * Position of this stop in the route's ordered sequence.
     *
     * Must be a positive integer starting from 1.
     * The Java service manages reordering logic.
     */
    stop_sequence INT NOT NULL,

    /*
     * Estimated arrival time at this stop, measured in minutes
     * from the route's departure at the first stop.
     *
     * Ex:
     *      stop_sequence=1, estimated_arrival_minutes=0   (departure)
     *      stop_sequence=2, estimated_arrival_minutes=15
     *      stop_sequence=3, estimated_arrival_minutes=30
     *
     * NULL means no time estimate has been recorded.
     */
    estimated_arrival_minutes INT,

    /*
     * Primary-key constraint.
     */
    CONSTRAINT pk_route_stops
        PRIMARY KEY (id),

    /*
     * Foreign key linking this stop to its parent route.
     */
    CONSTRAINT fk_route_stops_route
        FOREIGN KEY (route_id)
        REFERENCES arc.routes (id)
        ON DELETE CASCADE,

    /*
     * Prevents two stops on the same route from having
     * the same sequence number.
     *
     * Ex:
     *      Route RT-001 cannot have two stops at position 3.
     */
    CONSTRAINT ux_route_stops_sequence
        UNIQUE (route_id, stop_sequence),

    /*
     * Prevents two stops on the same route from having
     * the same name.
     *
     * Ex:
     *      Route RT-001 cannot list "SM North" twice.
     */
    CONSTRAINT ux_route_stops_name
        UNIQUE (route_id, stop_name),

    /*
     * Stop name must not be blank.
     */
    CONSTRAINT ck_route_stops_stop_name_not_blank
        CHECK (BTRIM(stop_name) <> ''),

    /*
     * Stop sequence must be a positive integer (1 or higher).
     */
    CONSTRAINT ck_route_stops_sequence_positive
        CHECK (stop_sequence > 0),

    /*
     * Estimated arrival minutes must be zero or positive if provided.
     * The first stop (departure) typically has 0 minutes.
     */
    CONSTRAINT ck_route_stops_arrival_minutes_not_negative
        CHECK (
            estimated_arrival_minutes IS NULL
            OR estimated_arrival_minutes >= 0
        )
);
