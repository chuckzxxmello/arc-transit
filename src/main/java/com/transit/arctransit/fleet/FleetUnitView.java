package com.transit.arctransit.fleet;

// view bus fleet unit via front-end user interface
public record FleetUnitView(
        Long id,
        String unitNumber,
        String plateNumber,
        String vehicleType,
        short capacity,
        String operationalStatus
) {}
