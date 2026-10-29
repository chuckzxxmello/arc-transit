package com.transit.arctransit.reporting;

import java.time.Instant;

public record OperationalReportView(
        Long id,
        String sourceType,
        String reportedBy,
        Instant reportedAt,
        Instant effectiveAt,
        String verificationStatus,
        String verifiedBy,
        Instant verifiedAt,
        String entityType,
        Long entityId,
        String description,
        String freshness,
        String verificationReason,
        Instant archivedAt
) {
}
