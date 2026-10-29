package com.transit.arctransit.incident;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// command for reporting a new operational incident
public record CreateIncidentCommand(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters.") @SafeText String title,

        @Size(max = 500, message = "Description must not exceed 500 characters.") @SafeText(multiline = true) String description,

        @NotNull(message = "Severity is required")
        @Size(max = 50, message = "Severity must not exceed 50 characters.") @SafeText String severity,

        @Size(max = 50, message = "Affected service type must not exceed 50 characters.") @SafeText String affectedServiceType,

        @Positive Long affectedEntityId,
        @NotNull java.time.Instant effectiveAt,
        @Positive Long operationalReportId

) {
    public CreateIncidentCommand(String title, String description, String severity,
             String affectedServiceType, Long affectedEntityId) {
        this(title, description, severity, affectedServiceType, affectedEntityId,
                java.time.Instant.now(), null);
    }
}
