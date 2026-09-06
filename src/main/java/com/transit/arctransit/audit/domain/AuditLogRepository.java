package com.transit.arctransit.audit.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

// data access for audit records
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // retrieve audit history for one entity
    Page<AuditLog> findByEntityNameAndEntityIdOrderByTimestampDesc(
            String entityName,
            Long entityId,
            Pageable pageable
    );
}