package com.transit.arctransit.dispatch;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Command to create a new dispatch assignment.
 */
public record CreateDispatchCommand(
        @NotNull(message = "Fleet unit ID is required") Long fleetUnitId,
        @NotNull(message = "Driver ID is required") Long driverId,
        @NotNull(message = "Route ID is required") Long routeId,
        @NotNull(message = "Dispatch date is required") LocalDate dispatchDate,
        @NotNull(message = "Scheduled departure time is required") Instant scheduledDeparture,
        Instant scheduledArrival,
        String notes
) {}
