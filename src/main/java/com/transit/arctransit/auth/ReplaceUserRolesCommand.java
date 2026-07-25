package com.transit.arctransit.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

/**
 * Command to completely replace the roles assigned to a user.
 */
public record ReplaceUserRolesCommand(
        @NotBlank(message = "Username cannot be blank") String username,
        @NotEmpty(message = "User must have at least one role") Set<String> roles
) {}
