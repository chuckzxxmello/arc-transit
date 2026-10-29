package com.transit.arctransit.common.ui;

import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.incident.CreateIncidentCommand;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationErrorsTest {
    @Test
    void validationMessagesExcludePathsAndRejectedValues() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            String input = "private".repeat(100);
            var violations = factory.getValidator().validate(new CreateIncidentCommand(input, null, "MEDIUM", null, null));
            String message = ApplicationErrors.message(new ConstraintViolationException(violations));
            assertTrue(message.contains("200"));
            assertFalse(message.contains(input));
            assertFalse(message.contains("CreateIncidentCommand"));
        }
    }

    @Test
    void expectedFailuresHaveActionableMessagesAndUnexpectedDetailsStayOnServer() {
        assertEquals("Choose a valid status.", ApplicationErrors.message(new CommandValidationException("Choose a valid status.")));
        assertFalse(ApplicationErrors.message(new DataIntegrityViolationException("private SQL detail")).contains("private"));
        assertFalse(ApplicationErrors.message(new IllegalStateException("private SQL detail")).contains("private"));
        assertTrue(ApplicationErrors.message(new ObjectOptimisticLockingFailureException("Record", 1L)).contains("another user"));
        assertTrue(ApplicationErrors.message(new AccessDeniedException("private policy")).contains("permission"));
    }
}
