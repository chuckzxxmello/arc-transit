package com.transit.arctransit.route;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Command to create a new route with its ordered stops.
 */
public record CreateRouteCommand(
        @NotBlank(message = "Route code cannot be blank") String routeCode,
        @NotBlank(message = "Route name cannot be blank") String routeName,
        String description,
        Integer estimatedDurationMinutes,
        List<StopEntry> stops
) {
    /**
     * Nested record representing a single stop in the create command.
     */
    public record StopEntry(
            String stopName,
            int stopSequence,
            Integer estimatedArrivalMinutes
    ) {}
}
