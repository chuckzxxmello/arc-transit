package com.transit.arctransit.route;

import java.util.List;

// detailed route representation
public record RouteView(

        Long id,
        String routeCode,
        String routeName,
        String description,
        String routeStatus,
        Integer estimatedDurationMinutes,
        List<RouteStopView> stops,
        java.time.Instant archivedAt

) {
    public RouteView(Long id, String routeCode, String routeName, String description,
                     String routeStatus, Integer estimatedDurationMinutes,
                     List<RouteStopView> stops) {
        this(id, routeCode, routeName, description, routeStatus,
                estimatedDurationMinutes, stops, null);
    }

    public RouteView {
        stops = stops == null
                ? List.of()
                : List.copyOf(stops);
    }
}
