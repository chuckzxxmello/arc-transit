package com.transit.arctransit.fleet;

// list the summary view of fleet management grids
public record FleetUnitSummaryView(
        Long id,
        String unitNumber,
        String plateNumber,
        short capacity,
        String operationalStatus
) {}
