package com.transit.arctransit.maintenance;

import jakarta.validation.constraints.Positive;
import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// command for requesting maintenance
public record CreateMaintenanceCommand(

        @NotNull(message = "Fleet unit ID is required")
        @Positive Long fleetUnitId,

        @NotBlank(message = "Maintenance type is required")
        @Size(max = 50, message = "Maintenance type must not exceed 50 characters.") @SafeText String maintenanceType,

        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must not exceed 500 characters.") @SafeText(multiline = true) String description,

        LocalDate scheduledDate

) {
}
