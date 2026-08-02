package com.transit.arctransit.route.domain;

/**
 * Operational lifecycle state of a route.
 *
 * Matches the database check constraint ck_routes_route_status
 * in V4__create_routes_and_stops.sql.
 */
public enum RouteStatus {

    /** The route is available for dispatch assignments. */
    ACTIVE,

    /** The route exists but is not currently operated. */
    INACTIVE
}
