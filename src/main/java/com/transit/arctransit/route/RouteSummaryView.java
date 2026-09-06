package com.transit.arctransit.route;

// compact route representation for list and grid views
public record RouteSummaryView(

        Long id,
        String routeCode,
        String routeName,
        int stopCount,
        Integer estimatedDurationMinutes,
        String routeStatus

) {
}