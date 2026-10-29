package com.transit.arctransit.audit;

import java.time.Instant;

public record AuditEventView(Long id, String actionName, String entityName,
                             Long entityId, String performedBy, Instant timestamp,
                             String outcome, String details) {
}
