package com.transit.arctransit.auth.security;

import com.transit.arctransit.audit.AuditRecordingService;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

// record Spring Security authentication events in the application audit trail.
@Component
public class AuthenticationEventListener {

    private static final long NO_TARGET_ID = 0L;
    private static final String LOGIN_SUCCESS = "USER_LOGIN_SUCCESS";
    private static final String LOGIN_FAILED = "USER_LOGIN_FAILED";
    private final AuditRecordingService auditRecordingService;

    public AuthenticationEventListener(AuditRecordingService auditRecordingService) {
        this.auditRecordingService = auditRecordingService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {

        String username = event.getAuthentication().getName();

        auditRecordingService.recordAction(
						                LOGIN_SUCCESS,
						                "AppUser",
						                NO_TARGET_ID,
						                "Authentication succeeded for user: " 
						                								+ username
        );
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {

        String username = event.getAuthentication().getName();
        String failureType = event.getClass().getSimpleName();

        auditRecordingService.recordAction(
						                LOGIN_FAILED,
						                "AppUser",
						                NO_TARGET_ID,
						                "Authentication failed for user: "
											                        + username
											                        + " ("
											                        + failureType
											                        + ")"
        );
    }
}