package com.transit.arctransit.route;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

// command for creating a route and its ordered stops
public record CreateRouteCommand(

        @NotBlank(message = "Route code cannot be blank")
        @Size(max = 30, message = "Route code must not exceed 30 characters.") @SafeText String routeCode,

        @NotBlank(message = "Route name cannot be blank")
        @Size(max = 100, message = "Route name must not exceed 100 characters.") @SafeText String routeName,

        @Size(max = 500, message = "Description must not exceed 500 characters.") @SafeText(multiline = true) String description,

        @Positive(message = "Estimated duration must be greater than zero")
        Integer estimatedDurationMinutes,

        List<@Valid StopEntry> stops,
        java.time.LocalDate effectiveFrom

) {

    public CreateRouteCommand(String routeCode, String routeName, String description,
                              Integer estimatedDurationMinutes, List<StopEntry> stops) {
        this(routeCode, routeName, description, estimatedDurationMinutes, stops, null);
    }

    public CreateRouteCommand {
        stops = stops == null
                ? List.of()
                : List.copyOf(stops);
    }

    // single stop within a route
    public record StopEntry(

            @NotBlank(message = "Stop name cannot be blank")
            @Size(max = 100, message = "Stop name must not exceed 100 characters.") @SafeText String stopName,

            @Positive(message = "Stop sequence must be greater than zero")
            int stopSequence,

            @PositiveOrZero(
                    message = "Estimated arrival minutes cannot be negative"
            )
            Integer estimatedArrivalMinutes

    ) {
    }
}
