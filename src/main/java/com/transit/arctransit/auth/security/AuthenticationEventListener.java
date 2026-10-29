package com.transit.arctransit.auth.security;

import com.transit.arctransit.audit.AuditRecordingService;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

// record Spring Security authentication events in the application audit trail.
@Component
public class AuthenticationEventListener {

    private final AuditRecordingService auditRecordingService;

    public AuthenticationEventListener(AuditRecordingService auditRecordingService) {
        this.auditRecordingService = auditRecordingService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {

        String username = event.getAuthentication().getName();

        auditRecordingService.recordAuthentication(username, true, null);
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {

        String username = event.getAuthentication().getName();
        String failureType = event.getClass().getSimpleName();

        auditRecordingService.recordAuthentication(username, false, failureType);
    }

    @EventListener
    public void onAuthorizationDenied(
            org.springframework.security.authorization.event.AuthorizationDeniedEvent<?> event) {
        if (!(event.getObject() instanceof org.aopalliance.intercept.MethodInvocation invocation)) {
            return;
        }
        String actor = "ANONYMOUS";
        try {
            var authentication = event.getAuthentication().get();
            if (authentication != null) {
                actor = authentication.getName();
            }
        } catch (org.springframework.security.core.AuthenticationException exception) {
            // An unauthenticated attempt has no principal to attribute.
        }
        auditRecordingService.recordDeniedAction(actor,
                invocation.getMethod().getDeclaringClass().getSimpleName()
                        + "." + invocation.getMethod().getName());
    }
}
