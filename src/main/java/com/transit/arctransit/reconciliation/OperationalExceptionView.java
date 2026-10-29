package com.transit.arctransit.reconciliation;

import java.time.Instant;

/**
 * Read-only view of an operational exception.
 */
public record OperationalExceptionView(
        Long id,
        String exceptionType,
        String severity,
        String affectedEntityType,
        Long affectedEntityId,
        String description,
        String exceptionStatus,
        Instant detectedAt,
        String acknowledgedBy,
        Instant acknowledgedAt,
        String resolvedBy,
        Instant resolvedAt,
        String resolutionNotes,
        String ruleCode
) {
    public OperationalExceptionView(Long id, String exceptionType, String severity,
            String affectedEntityType, Long affectedEntityId, String description,
            String exceptionStatus, Instant detectedAt, String acknowledgedBy,
            Instant acknowledgedAt, String resolvedBy, Instant resolvedAt, String resolutionNotes) {
        this(id, exceptionType, severity, affectedEntityType, affectedEntityId, description,
                exceptionStatus, detectedAt, acknowledgedBy, acknowledgedAt, resolvedBy,
                resolvedAt, resolutionNotes, exceptionType + "_" + affectedEntityType);
    }
}
