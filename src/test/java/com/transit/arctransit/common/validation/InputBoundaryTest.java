package com.transit.arctransit.common.validation;

import com.transit.arctransit.auth.CreateUserCommand;
import com.transit.arctransit.incident.CreateIncidentCommand;
import com.transit.arctransit.route.CreateRouteCommand;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class InputBoundaryTest {
    @Test
    void tierBoundariesAcceptTheLimitAndRejectTheNextCharacter() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (int length : List.of(50, 51)) {
                var account = new CreateUserCommand("r".repeat(length), "Valid899!", "Staff", null, Set.of("OPERATIONS_STAFF"));
                assertEquals(length == 50, validator.validateProperty(account, "username").isEmpty());
            }
            for (int length : List.of(100, 101)) {
                var route = new CreateRouteCommand("RT1", "r".repeat(length), null, 60, List.of());
                assertEquals(length == 100, validator.validateProperty(route, "routeName").isEmpty());
            }
            for (int length : List.of(200, 201)) {
                var incident = new CreateIncidentCommand("r".repeat(length), null, "MEDIUM", null, null);
                assertEquals(length == 200, validator.validateProperty(incident, "title").isEmpty());
            }
            for (int length : List.of(500, 501)) {
                var incident = new CreateIncidentCommand("Title", "r".repeat(length), "MEDIUM", null, null);
                assertEquals(length == 500, validator.validateProperty(incident, "description").isEmpty());
            }
        }
    }

    @Test
    void freeTextRetainsUnicodePunctuationAndMultilineNotesButRejectsControlCharacters() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var safe = new CreateIncidentCommand("O'Brien <route> - ñ", "Line 1\nLine 2\tDetails", "MEDIUM", null, null);
            assertTrue(validator.validate(safe).isEmpty());
            for (char control : new char[] {0, 8, 27, 127}) {
                var unsafe = new CreateIncidentCommand("Title", "Invalid" + control + "text", "MEDIUM", null, null);
                assertFalse(validator.validateProperty(unsafe, "description").isEmpty());
            }
        }
    }
}
