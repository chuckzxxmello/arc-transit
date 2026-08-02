package com.transit.arctransit.route;

import java.util.List;

/**
 * Immutable view of a route with its ordered stops.
 */
public record RouteView(
        Long id,
        String routeCode,
        String routeName,
        String description,
        String routeStatus,
        Integer estimatedDurationMinutes,
        List<RouteStopView> stops
) {}
