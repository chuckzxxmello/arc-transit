package com.transit.arctransit.fleet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Command to register a new bus in the fleet.
 *
 * Validation annotations are enforced by spring-boot-starter-validation
 * when the service method parameter is annotated with @Valid.
 *
 * Source: https://jakarta.ee/specifications/bean-validation/3.1/
 * (Ctrl+F: @NotBlank, @Positive)
 */
public record CreateFleetUnitCommand(
        @NotBlank(message = "Unit number cannot be blank") String unitNumber,
        @NotBlank(message = "Plate number cannot be blank") String plateNumber,
        @Positive(message = "Capacity must be greater than zero") short capacity
) {}
