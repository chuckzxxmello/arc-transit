package com.transit.arctransit.reporting;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record CreateOperationalReportCommand(
        @NotBlank @Size(max = 50, message = "Source type must not exceed 50 characters.") @SafeText String sourceType,
        @NotBlank @Size(max = 50, message = "Entity type must not exceed 50 characters.") @SafeText String entityType,
        @NotNull @Positive Long entityId,
        @NotNull Instant effectiveAt,
        @NotBlank @Size(max = 500, message = "Description must not exceed 500 characters.") @SafeText(multiline = true) String description
) {
}
