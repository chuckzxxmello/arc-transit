package com.transit.arctransit.fleet;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;

// to update an existing bust fleet unit details
public record UpdateFleetUnitCommand(
        @NotNull(message = "Fleet unit ID is required") @Positive Long id,
        @NotBlank(message = "Unit number cannot be blank") @Size(max = 30, message = "Unit number must not exceed 30 characters.") @SafeText String unitNumber,
        @NotBlank(message = "Plate number cannot be blank") @Size(max = 20, message = "Plate number must not exceed 20 characters.") @SafeText String plateNumber,
        @Positive(message = "Capacity must be greater than zero")
        @Max(value = 80, message = "Capacity must not exceed 80 passengers.") short capacity
) {}
