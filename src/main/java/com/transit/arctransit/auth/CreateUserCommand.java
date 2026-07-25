package com.transit.arctransit.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

/**
 * Command to create a new staff account.
 */
public record CreateUserCommand(
        @NotBlank(message = "Username cannot be blank") String username,
        @NotBlank(message = "Password cannot be blank") String password,
        @NotBlank(message = "Display name cannot be blank") String displayName,
        String email,
        @NotEmpty(message = "User must have at least one role") Set<String> roles
) {}
