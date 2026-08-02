package com.transit.arctransit.route;

/**
 * Immutable view of a single route stop for frontend consumption.
 */
public record RouteStopView(
        String stopName,
        int stopSequence,
        Integer estimatedArrivalMinutes
) {}
