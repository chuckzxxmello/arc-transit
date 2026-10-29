package com.transit.arctransit.fleet;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateFleetStateCommand(
        @NotNull @Positive Long fleetUnitId,
        @NotBlank @Size(max = 50) @SafeText String administrativeStatus,
        @NotBlank @Size(max = 50) @SafeText String availability,
        @Size(max = 500) @SafeText(multiline = true) String reason) {
}
