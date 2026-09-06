package com.transit.arctransit.maintenance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// command for requesting maintenance
public record CreateMaintenanceCommand(

        @NotNull(message = "Fleet unit ID is required")
        Long fleetUnitId,

        @NotBlank(message = "Maintenance type is required")
        String maintenanceType,

        @NotBlank(message = "Description is required")
        String description,

        LocalDate scheduledDate

) {
}