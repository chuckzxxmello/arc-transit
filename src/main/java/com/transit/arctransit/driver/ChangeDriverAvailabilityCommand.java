package com.transit.arctransit.driver;

import jakarta.validation.constraints.Positive;
import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ChangeDriverAvailabilityCommand(
        @NotNull @Positive Long driverId,
        @NotBlank @Size(max = 50, message = "Availability must not exceed 50 characters.") @SafeText String availability,
        @NotNull Instant effectiveAt,
        @NotBlank @Size(max = 500, message = "Reason must not exceed 500 characters.") @SafeText(multiline = true) String reason
) {}
