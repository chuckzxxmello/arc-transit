package com.transit.arctransit.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// command for reporting a new operational incident
public record CreateIncidentCommand(

        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotNull(message = "Severity is required")
        String severity,

        String affectedServiceType,

        Long affectedEntityId

) {
}