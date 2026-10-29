package com.transit.arctransit.auth;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import java.util.Set;

//Command to create a new staff account.
public record CreateUserCommand(
        @NotBlank(message = "Username cannot be blank") @Size(max = 50, message = "Username must not exceed 50 characters.") @SafeText String username,
        @NotBlank(message = "Password cannot be blank")
        @Pattern(
            regexp = CreateUserCommand.PASSWORD_PATTERN,
            message = PasswordPolicy.MESSAGE
        )
        
        String password,
        @NotBlank(message = "Display name cannot be blank") @Size(max = 100, message = "Display name must not exceed 100 characters.") @SafeText String displayName,
        @Email @Size(max = 200, message = "Email must not exceed 200 characters.") @SafeText String email,
        @NotEmpty(message = "User must have at least one role") @Size(max = 2) Set<@NotBlank @Size(max = 40) @SafeText String> roles
) {
    public static final String PASSWORD_PATTERN = PasswordPolicy.PATTERN;
}
