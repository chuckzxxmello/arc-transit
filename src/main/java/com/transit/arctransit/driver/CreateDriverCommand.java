package com.transit.arctransit.driver;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Command to register a new driver.
 */
public record CreateDriverCommand(
        @NotBlank(message = "Employee number cannot be blank") String employeeNumber,
        @NotBlank(message = "First name cannot be blank") String firstName,
        @NotBlank(message = "Last name cannot be blank") String lastName,
        @NotBlank(message = "License number cannot be blank") String licenseNumber,
        @NotBlank(message = "License type cannot be blank") String licenseType,
        @NotNull(message = "License expiry date is required") LocalDate licenseExpiryDate,
        String contactNumber
) {}
