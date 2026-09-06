package com.transit.arctransit.audit.application;

// audit module

import com.transit.arctransit.audit.AuditRecordingService;
import com.transit.arctransit.audit.domain.AuditLog;
import com.transit.arctransit.audit.domain.AuditLogRepository;

// spring security

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// spring

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// audit application service

@Service
@Transactional
public class AppAuditRecordingService
        implements AuditRecordingService {

    // dependencies

    private final AuditLogRepository auditLogRepository;

    public AppAuditRecordingService(
            AuditLogRepository auditLogRepository) {

        this.auditLogRepository =
                auditLogRepository;
    }

    // record audit action

    @Override
    public void recordAction(
            String actionName,
            String entityName,
            Long entityId,
            String details) {

        String performedBy =
                resolveCurrentUsername();

        AuditLog auditLog =
                new AuditLog(
                        actionName,
                        entityName,
                        entityId,
                        performedBy,
                        details
                );

        auditLogRepository.save(auditLog);
    }

    // resolve current authenticated user

    private String resolveCurrentUsername() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication
                        instanceof AnonymousAuthenticationToken) {

            return "SYSTEM";
        }

        String username =
                authentication.getName();

        if (username == null || username.isBlank()) {
            return "SYSTEM";
        }

        return username;
    }
}