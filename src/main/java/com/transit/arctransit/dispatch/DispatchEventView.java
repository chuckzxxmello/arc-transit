package com.transit.arctransit.dispatch;

import java.time.Instant;

public record DispatchEventView(Long assignmentId, String previousStatus, String newStatus,
                                Instant effectiveAt, Instant recordedAt, String actor,
                                String sourceType) {
}
