package com.transit.arctransit.route;

/**
 * Summary view for route management list grids.
 */
public record RouteSummaryView(
        Long id,
        String routeCode,
        String routeName,
        int stopCount,
        Integer estimatedDurationMinutes,
        String routeStatus
) {}
