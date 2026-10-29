package com.transit.arctransit.fleet;

import java.time.Instant;

public record VehicleAvailabilityHistoryView(
        Long id,
        String previousAvailability,
        String availability,
        Instant effectiveAt,
        Instant recordedAt,
        String recordedBy,
        String sourceType,
        String verificationStatus,
        String reason
) {}