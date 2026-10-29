package com.transit.arctransit.reporting;

import java.time.Instant;

public record ReportVerificationView(
        String previousStatus,
        String newStatus,
        String recordedBy,
        Instant recordedAt,
        String reason
) {
}
