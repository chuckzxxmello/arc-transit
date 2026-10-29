package com.transit.arctransit.route;

import java.time.LocalDate;
import java.util.List;

public record RouteVersionView(
        Long id,
        Long routeId,
        String routeCode,
        String routeName,
        String description,
        Integer estimatedDurationMinutes,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean legacySnapshot,
        List<RouteStopView> stops) {

    public RouteVersionView {
        stops = List.copyOf(stops);
    }

    public boolean isEffectiveOn(LocalDate date) {
        return date != null && !date.isBefore(effectiveFrom)
                && (effectiveTo == null || date.isBefore(effectiveTo));
    }
}
