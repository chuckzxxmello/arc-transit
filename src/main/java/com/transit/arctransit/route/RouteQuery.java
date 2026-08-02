package com.transit.arctransit.route;

/**
 * Query parameters for searching routes.
 */
public record RouteQuery(
        String routeCodeFilter,
        String statusFilter
) {}
