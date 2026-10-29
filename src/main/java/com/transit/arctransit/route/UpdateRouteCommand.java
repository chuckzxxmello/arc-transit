package com.transit.arctransit.route;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

// command for updating an existing route and replacing its stops

public record UpdateRouteCommand(

        @NotNull(message = "Route ID is required")
        @Positive Long id,

        @NotBlank(message = "Route code cannot be blank")
        @Size(max = 30, message = "Route code must not exceed 30 characters.") @SafeText String routeCode,

        @NotBlank(message = "Route name cannot be blank")
        @Size(max = 100, message = "Route name must not exceed 100 characters.") @SafeText String routeName,

        @Size(max = 500, message = "Description must not exceed 500 characters.") @SafeText(multiline = true) String description,

        @Positive(message = "Estimated duration must be greater than zero")
        Integer estimatedDurationMinutes,

        List<CreateRouteCommand.@Valid StopEntry> stops,
        java.time.LocalDate effectiveFrom,
        java.time.LocalDate effectiveTo

) {

    public UpdateRouteCommand(Long id, String routeCode, String routeName, String description,
                              Integer estimatedDurationMinutes,
                              List<CreateRouteCommand.StopEntry> stops) {
        this(id, routeCode, routeName, description, estimatedDurationMinutes, stops, null, null);
    }

    public UpdateRouteCommand {
        stops = stops == null
                ? List.of()	
                : List.copyOf(stops);
    }
}
