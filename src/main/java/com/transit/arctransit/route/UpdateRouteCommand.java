package com.transit.arctransit.route;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Command to update an existing route and replace its stops.
 */
public record UpdateRouteCommand(
        @NotNull(message = "Route ID is required") Long id,
        @NotBlank(message = "Route code cannot be blank") String routeCode,
        @NotBlank(message = "Route name cannot be blank") String routeName,
        String description,
        Integer estimatedDurationMinutes,
        List<CreateRouteCommand.StopEntry> stops
) {}
