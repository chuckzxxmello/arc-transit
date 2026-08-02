package com.transit.arctransit.driver;

/**
 * Immutable view of a driver for frontend consumption.
 */
public record DriverView(
        Long id,
        String employeeNumber,
        String firstName,
        String lastName,
        String licenseNumber,
        String licenseType,
        String licenseExpiryDate,
        String contactNumber,
        String employmentStatus,
        boolean licenseExpired
) {}
