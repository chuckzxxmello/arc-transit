package com.transit.arctransit.fleet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

// command to register a new bus in the fleet.
public record CreateFleetUnitCommand(
        @NotBlank(message = "Unit number cannot be blank") String unitNumber,
        @NotBlank(message = "Plate number cannot be blank") String plateNumber,
        @Positive(message = "Capacity must be greater than zero") short capacity
) {}
