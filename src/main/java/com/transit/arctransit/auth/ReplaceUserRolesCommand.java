package com.transit.arctransit.auth;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

// Command to completely replace the roles assigned to a user.
public record ReplaceUserRolesCommand(
        @NotBlank(message = "Username cannot be blank") @Size(max = 50, message = "Username must not exceed 50 characters.") @SafeText String username,
        @NotEmpty(message = "User must have at least one role") @Size(max = 2) Set<@NotBlank @Size(max = 40) @SafeText String> roles
) {}
