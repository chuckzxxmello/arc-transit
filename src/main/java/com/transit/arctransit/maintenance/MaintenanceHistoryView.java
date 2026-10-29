package com.transit.arctransit.maintenance;

import java.time.Instant;

public record MaintenanceHistoryView(
        Long id, String previousStatus, String status,
        Instant recordedAt, String recordedBy, String reason
) {}