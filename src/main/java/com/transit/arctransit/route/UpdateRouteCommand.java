package com.transit.arctransit.route;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

// command for updating an existing route and replacing its stops

public record UpdateRouteCommand(

        @NotNull(message = "Route ID is required")
        Long id,

        @NotBlank(message = "Route code cannot be blank")
        String routeCode,

        @NotBlank(message = "Route name cannot be blank")
        String routeName,

        String description,

        @Positive(message = "Estimated duration must be greater than zero")
        Integer estimatedDurationMinutes,

        List<CreateRouteCommand.@Valid StopEntry> stops

) {

    public UpdateRouteCommand {
        stops = stops == null
                ? List.of()	
                : List.copyOf(stops);
    }
}