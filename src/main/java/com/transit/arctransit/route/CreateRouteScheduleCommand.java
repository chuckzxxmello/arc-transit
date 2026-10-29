package com.transit.arctransit.route;

import jakarta.validation.constraints.Positive;
import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateRouteScheduleCommand(
        @NotNull @Positive Long routeId,
        @NotBlank @Size(max = 100, message = "Name must not exceed 100 characters.") @SafeText String name,
        @NotNull LocalTime plannedDeparture,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo) {
}
