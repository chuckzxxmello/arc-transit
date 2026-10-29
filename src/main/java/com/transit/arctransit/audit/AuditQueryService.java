package com.transit.arctransit.audit;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditQueryService {
    Page<AuditEventView> searchAudit(@Valid AuditQuery query, Pageable pageable);
}
