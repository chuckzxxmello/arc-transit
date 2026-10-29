package com.transit.arctransit.driver;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// to register a new driver
public record CreateDriverCommand(
        @NotBlank(message = "Employee number cannot be blank") @Size(max = 30, message = "Employee number must not exceed 30 characters.") @SafeText String employeeNumber,
        @NotBlank(message = "First name cannot be blank") @Size(max = 50, message = "First name must not exceed 50 characters.") @SafeText String firstName,
        @NotBlank(message = "Last name cannot be blank") @Size(max = 50, message = "Last name must not exceed 50 characters.") @SafeText String lastName,
        @NotBlank(message = "License number cannot be blank") @Size(max = 50, message = "License number must not exceed 50 characters.") @SafeText String licenseNumber,
        @NotBlank(message = "License type cannot be blank") @Size(max = 50, message = "License type must not exceed 50 characters.") @SafeText String licenseType,
        @NotNull(message = "License expiry date is required") LocalDate licenseExpiryDate,
        @jakarta.validation.constraints.Pattern(regexp = "(?:|09[0-9]{9})", message = "Use an 11-digit mobile number starting with 09, such as 09691477344.") String contactNumber
) {}
