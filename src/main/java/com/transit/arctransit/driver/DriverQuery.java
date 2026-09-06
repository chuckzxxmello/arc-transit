package com.transit.arctransit.driver;

// query parameters for searching drivers
public record DriverQuery(
        String employeeNumberFilter,
        String statusFilter
) {}
