package com.transit.arctransit.driver;

import java.time.Instant;

public record DriverAvailabilityHistoryView(
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