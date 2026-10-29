package com.transit.arctransit.fleet;

import java.time.Instant;

// view bus fleet unit via front-end user interface
public record FleetUnitView(
        Long id,
        String unitNumber,
        String plateNumber,
        String vehicleType,
        short capacity,
        String operationalStatus,
        String administrativeStatus,
        String availability,
        Instant archivedAt
) {
    public FleetUnitView(Long id, String unitNumber, String plateNumber,
            String vehicleType, short capacity, String operationalStatus) {
        this(id, unitNumber, plateNumber, vehicleType, capacity, operationalStatus,
                "INACTIVE".equals(operationalStatus) ? "INACTIVE" : "ACTIVE",
                switch (operationalStatus) {
                    case "ACTIVE" -> "AVAILABLE";
                    case "UNDER_MAINTENANCE" -> "MAINTENANCE";
                    case "OUT_OF_SERVICE" -> "OUT_OF_SERVICE";
                    default -> "UNKNOWN";
                }, null);
    }
}