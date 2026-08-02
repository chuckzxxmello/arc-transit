package com.transit.arctransit.fleet;

/**
 * Summary view for fleet management list grids.
 */
public record FleetUnitSummaryView(
        Long id,
        String unitNumber,
        String plateNumber,
        short capacity,
        String operationalStatus
) {}
