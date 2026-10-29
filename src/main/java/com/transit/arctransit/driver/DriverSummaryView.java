package com.transit.arctransit.driver;

import java.time.Instant;

// summary view for driver management list grids
public record DriverSummaryView(
        Long id,
        String employeeNumber,
        String fullName,
        String licenseNumber,
        String licenseExpiryDate,
        String employmentStatus,
        boolean licenseExpired,
        String availability,
        Instant archivedAt
) {
    public DriverSummaryView(Long id, String employeeNumber, String fullName,
            String licenseNumber, String licenseExpiryDate, String employmentStatus,
            boolean licenseExpired) {
        this(id, employeeNumber, fullName, licenseNumber, licenseExpiryDate,
                employmentStatus, licenseExpired,
                "ACTIVE".equals(employmentStatus) ? "AVAILABLE" : "UNAVAILABLE", null);
    }
}