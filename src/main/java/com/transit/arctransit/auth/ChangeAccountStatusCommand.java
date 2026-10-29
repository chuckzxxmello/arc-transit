package com.transit.arctransit.auth;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;

// to change the operational status of an account.
public record ChangeAccountStatusCommand(
        @NotBlank(message = "Username cannot be blank") @Size(max = 50, message = "Username must not exceed 50 characters.") @SafeText String username,
        @NotBlank(message = "New status cannot be blank") @Size(max = 50, message = "New status must not exceed 50 characters.") @SafeText String newStatus
) {}
