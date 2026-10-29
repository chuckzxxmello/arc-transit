package com.transit.arctransit.incident;

import java.time.Instant;

public record IncidentEventView(String previousStatus, String newStatus, Instant effectiveAt,
        Instant recordedAt, String recordedBy, String reason) {
}
