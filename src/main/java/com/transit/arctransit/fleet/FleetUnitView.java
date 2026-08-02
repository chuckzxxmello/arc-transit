package com.transit.arctransit.fleet;

/**
 * Immutable view of a fleet unit for frontend consumption.
 *
 * Uses Java 21 record for inherent immutability and thread safety.
 * Source: https://docs.oracle.com/en/java/javase/21/language/records.html
 * (Ctrl+F: record class)
 */
public record FleetUnitView(
        Long id,
        String unitNumber,
        String plateNumber,
        String vehicleType,
        short capacity,
        String operationalStatus
) {}
