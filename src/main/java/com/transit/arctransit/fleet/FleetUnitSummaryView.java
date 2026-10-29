package com.transit.arctransit.fleet;

import java.time.Instant;

// list the summary view of fleet management grids
public record FleetUnitSummaryView(
        Long id,
        String unitNumber,
        String plateNumber,
        short capacity,
        String operationalStatus,
        String administrativeStatus,
        String availability,
        Instant archivedAt
) {
    public FleetUnitSummaryView(Long id, String unitNumber, String plateNumber,
            short capacity, String operationalStatus) {
        this(id, unitNumber, plateNumber, capacity, operationalStatus,
                new FleetUnitView(id, unitNumber, plateNumber, "BUS", capacity,
                        operationalStatus).administrativeStatus(),
                new FleetUnitView(id, unitNumber, plateNumber, "BUS", capacity,
                        operationalStatus).availability(), null);
    }
}