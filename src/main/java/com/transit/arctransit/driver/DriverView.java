package com.transit.arctransit.driver;

import java.time.Instant;

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
        boolean licenseExpired,
        String availability,
        Instant archivedAt
) {
    public DriverView(Long id, String employeeNumber, String firstName,
            String lastName, String licenseNumber, String licenseType,
            String licenseExpiryDate, String contactNumber, String employmentStatus,
            boolean licenseExpired) {
        this(id, employeeNumber, firstName, lastName, licenseNumber, licenseType,
                licenseExpiryDate, contactNumber, employmentStatus, licenseExpired,
                "ACTIVE".equals(employmentStatus) ? "AVAILABLE" : "UNAVAILABLE", null);
    }
}