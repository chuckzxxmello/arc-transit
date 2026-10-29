package com.transit.arctransit.auth;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UpdateUserCommand(
        @NotNull @Positive Long id,
        @NotNull @PositiveOrZero Long version,
        @NotBlank @Size(max = 50) @SafeText String username,
        @NotBlank @Size(max = 100) @SafeText String displayName,
        @Email @Size(max = 200) @SafeText String email,
        @NotEmpty @Size(max = 2) Set<@NotBlank @Size(max = 40) @SafeText String> roles
) {
}
