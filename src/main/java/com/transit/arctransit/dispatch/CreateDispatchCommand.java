package com.transit.arctransit.dispatch;

import jakarta.validation.constraints.Positive;
import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

// command for creating a dispatch assignment
public record CreateDispatchCommand(

        @NotNull(message = "Fleet unit ID is required") @Positive Long fleetUnitId,
        @NotNull(message = "Driver ID is required") @Positive Long driverId,
        @NotNull(message = "Route ID is required") @Positive Long routeId,
        @NotNull(message = "Dispatch date is required") LocalDate dispatchDate,
        @NotNull(message = "Scheduled departure time is required") Instant scheduledDeparture,
        Instant scheduledArrival,
        @Size(max = 500, message = "Notes must not exceed 500 characters.") @SafeText(multiline = true) String notes,
        @Positive Long scheduleId

) {
    public CreateDispatchCommand(Long fleetUnitId, Long driverId, Long routeId,
                                 LocalDate dispatchDate, Instant scheduledDeparture,
                                 Instant scheduledArrival, String notes) {
        this(fleetUnitId, driverId, routeId, dispatchDate, scheduledDeparture,
                scheduledArrival, notes, null);
    }
}
