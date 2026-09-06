package com.transit.arctransit.maintenance;

import java.time.Instant;
import java.time.LocalDate;

// immutable maintenance view
public record MaintenanceRecordView(

        Long id,

        Long fleetUnitId,

        String maintenanceType,

        String maintenanceStatus,

        String description,

        LocalDate scheduledDate,

        Instant startedAt,

        Instant completedAt,

        String technicianNotes,

        Instant createdAt

) {
}