package com.transit.arctransit.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Command to change the operational status of an account.
 */
public record ChangeAccountStatusCommand(
        @NotBlank(message = "Username cannot be blank") String username,
        @NotBlank(message = "New status cannot be blank") String newStatus
) {}
