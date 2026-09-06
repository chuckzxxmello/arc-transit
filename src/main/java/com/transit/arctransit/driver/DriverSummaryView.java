package com.transit.arctransit.driver;

// summary view for driver management list grids
public record DriverSummaryView(
        Long id,
        String employeeNumber,
        String fullName,
        String licenseNumber,
        String licenseExpiryDate,
        String employmentStatus,
        boolean licenseExpired
) {}
