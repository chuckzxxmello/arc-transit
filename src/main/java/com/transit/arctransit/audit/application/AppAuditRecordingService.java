package com.transit.arctransit.audit.application;

// audit module

import com.transit.arctransit.audit.AuditRecordingService;
import com.transit.arctransit.audit.AuditQueryService;
import com.transit.arctransit.audit.AuditQuery;
import com.transit.arctransit.audit.AuditEventView;
import com.transit.arctransit.common.exception.CommandValidationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Propagation;
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
@org.springframework.validation.annotation.Validated
@Transactional
public class AppAuditRecordingService
        implements AuditRecordingService, AuditQueryService {

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

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDeniedAction(String username, String action) {
        String actor = username == null || username.isBlank() ? "ANONYMOUS" : username;
        auditLogRepository.save(new AuditLog("AUTHORIZATION_DENIED", "ApplicationAction", 0L,
                actor.substring(0, Math.min(actor.length(), 100)), action, "DENIED"));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String actionName, String entityName, Long entityId, String details) {
        auditLogRepository.save(new AuditLog(actionName, entityName, entityId,
                resolveCurrentUsername(), details, "FAILURE"));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuthentication(String username, boolean successful, String failureType) {
        String actor = username == null || username.isBlank() ? "UNKNOWN" : username;
        if (actor.length() > 100) {
            actor = actor.substring(0, 100);
        }
        auditLogRepository.save(new AuditLog(
                successful ? "USER_LOGIN_SUCCESS" : "USER_LOGIN_FAILED", "AppUser", 0L,
                actor, successful ? "Authentication succeeded." : "Authentication failed (" + failureType + ").",
                successful ? "SUCCESS" : "FAILURE"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Page<AuditEventView> searchAudit(AuditQuery query, Pageable pageable) {
        if (query != null && query.from() != null && query.to() != null
                && !query.from().isBefore(query.to())) {
            throw new CommandValidationException("Audit start time must be before end time.");
        }
        Specification<AuditLog> specification = (root, criteria, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (query != null) {
                if (query.actor() != null && !query.actor().isBlank()) {
                    predicates.add(builder.equal(root.get("performedBy"), query.actor().trim()));
                }
                if (query.action() != null && !query.action().isBlank()) {
                    predicates.add(builder.equal(root.get("actionName"), query.action().trim()));
                }
                if (query.entityName() != null && !query.entityName().isBlank()) {
                    predicates.add(builder.equal(root.get("entityName"), query.entityName().trim()));
                }
                if (query.entityId() != null) {
                    predicates.add(builder.equal(root.get("entityId"), query.entityId()));
                }
                if (query.outcome() != null && !query.outcome().isBlank()) {
                    predicates.add(builder.equal(root.get("outcome"), query.outcome().trim()));
                }
                if (query.from() != null) {
                    predicates.add(builder.greaterThanOrEqualTo(root.get("timestamp"), query.from()));
                }
                if (query.to() != null) {
                    predicates.add(builder.lessThan(root.get("timestamp"), query.to()));
                }
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return auditLogRepository.findAll(specification, pageable).map(log ->
                new AuditEventView(log.getId(), log.getActionName(), log.getEntityName(),
                        log.getEntityId(), log.getPerformedBy(), log.getTimestamp(),
                        log.getOutcome(), log.getDetails()));
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