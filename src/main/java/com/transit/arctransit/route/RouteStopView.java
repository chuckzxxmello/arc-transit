package com.transit.arctransit.route;

// view a single route stop for user interface view
public record RouteStopView(
        String stopName,
        int stopSequence,
        Integer estimatedArrivalMinutes
) {}
