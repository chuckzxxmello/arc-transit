package com.transit.arctransit.audit;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record AuditQuery(@Size(max = 200) @SafeText String actor, @Size(max = 200) @SafeText String action, @Size(max = 200) @SafeText String entityName,
                         Long entityId, @Size(max = 200) @SafeText String outcome, Instant from, Instant to) {
}
