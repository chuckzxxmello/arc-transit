package com.transit.arctransit.incident;

import java.time.Instant;

// immutable incident view for UI and cross-module consumption
public record IncidentView(

        Long id,

        String title,

        String description,

        String severity,

        String status,

        String affectedServiceType,

        Long affectedEntityId,

        String reportedBy,

        Instant reportedAt,

        Instant acknowledgedAt,

        Instant resolvedAt,

        String resolutionNotes,
        Instant effectiveAt,
        String sourceType,
        Long operationalReportId,
        Long maintenanceRecordId

) {
}