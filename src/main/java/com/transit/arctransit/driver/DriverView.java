package com.transit.arctransit.driver;

// view of driver in UI
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
