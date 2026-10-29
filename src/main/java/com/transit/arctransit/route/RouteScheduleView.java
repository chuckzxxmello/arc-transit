package com.transit.arctransit.route;

import java.time.LocalDate;
import java.time.LocalTime;

public record RouteScheduleView(Long id, Long routeId, String name, LocalTime plannedDeparture,
                                LocalDate effectiveFrom, LocalDate effectiveTo, boolean active) {

    public boolean isEffectiveOn(LocalDate date) {
        return date != null && !date.isBefore(effectiveFrom)
                && (effectiveTo == null || date.isBefore(effectiveTo));
    }
}
