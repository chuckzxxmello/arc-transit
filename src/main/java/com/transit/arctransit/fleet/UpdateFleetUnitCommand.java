package com.transit.arctransit.fleet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Command to update an existing fleet unit's details.
 */
public record UpdateFleetUnitCommand(
        @NotNull(message = "Fleet unit ID is required") Long id,
        @NotBlank(message = "Unit number cannot be blank") String unitNumber,
        @NotBlank(message = "Plate number cannot be blank") String plateNumber,
        @Positive(message = "Capacity must be greater than zero") short capacity
) {}
