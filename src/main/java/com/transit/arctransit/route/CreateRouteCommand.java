package com.transit.arctransit.route;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

// command for creating a route and its ordered stops
public record CreateRouteCommand(

        @NotBlank(message = "Route code cannot be blank")
        String routeCode,

        @NotBlank(message = "Route name cannot be blank")
        String routeName,

        String description,

        @Positive(message = "Estimated duration must be greater than zero")
        Integer estimatedDurationMinutes,

        List<@Valid StopEntry> stops

) {

    public CreateRouteCommand {
        stops = stops == null
                ? List.of()
                : List.copyOf(stops);
    }

    // single stop within a route
    public record StopEntry(

            @NotBlank(message = "Stop name cannot be blank")
            String stopName,

            @Positive(message = "Stop sequence must be greater than zero")
            int stopSequence,

            @PositiveOrZero(
                    message = "Estimated arrival minutes cannot be negative"
            )
            Integer estimatedArrivalMinutes

    ) {
    }
}