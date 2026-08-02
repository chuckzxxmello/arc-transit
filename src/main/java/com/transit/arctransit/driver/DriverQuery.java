package com.transit.arctransit.driver;

/**
 * Query parameters for searching drivers.
 */
public record DriverQuery(
        String employeeNumberFilter,
        String statusFilter
) {}
