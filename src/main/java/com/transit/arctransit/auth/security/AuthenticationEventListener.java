package com.transit.arctransit.auth.security;

import com.transit.arctransit.audit.AuditRecordingService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Event listener that captures Spring Security authentication events and
 * records success and failure attempts to the audit log for OWASP A09 security monitoring.
 */
@Component
public class AuthenticationEventListener {

    private final AuditRecordingService auditRecordingService;

    public AuthenticationEventListener(AuditRecordingService auditRecordingService) {
        this.auditRecordingService = auditRecordingService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        auditRecordingService.recordAction(
                "USER_LOGIN_SUCCESS",
                "AppUser",
                0L,
                "Authentication succeeded for user: " + username
        );
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String exceptionMessage = event.getException().getMessage();
        auditRecordingService.recordAction(
                "USER_LOGIN_FAILED",
                "AppUser",
                0L,
                "Authentication failed for user: " + username + " (Reason: " + exceptionMessage + ")"
        );
    }
}
