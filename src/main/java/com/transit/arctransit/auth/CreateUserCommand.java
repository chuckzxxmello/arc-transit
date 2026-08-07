package com.transit.arctransit.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

import jakarta.validation.constraints.Pattern;

/**
 * Command to create a new staff account.
 */
public record CreateUserCommand(
        @NotBlank(message = "Username cannot be blank") String username,
        @NotBlank(message = "Password cannot be blank")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must be at least 8 characters long and include uppercase, lowercase, digit, and special character."
        )
        String password,
        @NotBlank(message = "Display name cannot be blank") String displayName,
        String email,
        @NotEmpty(message = "User must have at least one role") Set<String> roles
) {}
