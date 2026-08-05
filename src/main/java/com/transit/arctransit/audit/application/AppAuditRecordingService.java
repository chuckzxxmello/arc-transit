package com.transit.arctransit.audit.application;

import com.transit.arctransit.audit.AuditRecordingService;
import com.transit.arctransit.audit.domain.AuditLog;
import com.transit.arctransit.audit.domain.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppAuditRecordingService implements AuditRecordingService {

    private final AuditLogRepository auditLogRepository;

    public AppAuditRecordingService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void recordAction(String actionName, String entityName, Long entityId, String details) {
        String username = "SYSTEM";
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            username = authentication.getName();
        }

        AuditLog log = new AuditLog(actionName, entityName, entityId, username, details);
        auditLogRepository.save(log);
    }
}
