package com.transit.arctransit.auth;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    @Test
    void minimumAndMaximumLengthHaveSeparateMessages() {
        assertEquals("Password must be at least 8 characters long.", PasswordPolicy.validationMessage("Short1!"));
        assertEquals("Password must not exceed 16 characters.",
                PasswordPolicy.validationMessage("A1!" + "b".repeat(70)));
    }

    @Test
    void accountValidationAndPolicyAcceptStandardPunctuationConsistently() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (char symbol = '!'; symbol <= '~'; symbol++) {
                if (Character.isLetterOrDigit(symbol)) {
                    continue;
                }
                String password = "Transit899" + symbol;
                assertTrue(PasswordPolicy.isValid(password), "Punctuation code " + (int) symbol);
                var command = new CreateUserCommand("test.staff", password, "Test Staff", null,
                        Set.of("OPERATIONS_STAFF"));
                assertTrue(validator.validateProperty(command, "password").isEmpty());
                assertNull(PasswordPolicy.validationMessage(password));
            }
        }
    }

    @Test
    void rejectsMissingGroupsAndUnsupportedCharactersWithoutEchoingPassword() {
        List<String> invalid = List.of("", "Short1!", "usertest123", "UPPERCASE1!", "NoNumbers!!",
                "NoSymbols123", "Has space1!", "Has\ttab1!", "Has\nline1!", "Accent\u00e91!",
                "A1!" + "b".repeat(70));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            for (String password : invalid) {
                assertFalse(PasswordPolicy.isValid(password));
                assertNotNull(PasswordPolicy.validationMessage(password));
                assertEquals("weak", PasswordPolicy.estimatedStrength(password));
                var command = new CreateUserCommand("test.staff", password, "Test Staff", null,
                        Set.of("OPERATIONS_STAFF"));
                assertFalse(factory.getValidator().validateProperty(command, "password").isEmpty());
            }
        }
        assertFalse(PasswordPolicy.isValid(null));
        assertEquals("Password is required.", PasswordPolicy.validationMessage(null));
        assertFalse(PasswordPolicy.validationMessage("usertest123").contains("usertest123"));
    }

    @Test
    void lengthBoundariesAndReportedHashExampleAreAccepted() {
        assertTrue(PasswordPolicy.isValid("Lime89!?"));
        assertTrue(PasswordPolicy.isValid("A1!" + "b".repeat(13)));
        assertTrue(PasswordPolicy.isValid("Arctest899!@#"));
    }

    @Test
    void estimatesFourLevelsAndDoesNotRateCommonOrRepeatedPasswordsAsStrong() {
        assertEquals("very strong", PasswordPolicy.estimatedStrength("Us3!****aB9#cD2$"));
        assertEquals("weak", PasswordPolicy.estimatedStrength("r".repeat(500)));
        assertEquals("weak", PasswordPolicy.estimatedStrength("short"));
        assertEquals("moderate", PasswordPolicy.estimatedStrength("Lime89!?"));
        assertEquals("strong", PasswordPolicy.estimatedStrength("Maple7!River2"));
        assertEquals("very strong", PasswordPolicy.estimatedStrength("Cedar9#Sea4!Moon"));
        assertEquals("weak", PasswordPolicy.estimatedStrength("Password123456789!"));
        assertEquals("weak", PasswordPolicy.estimatedStrength("Ab1!Ab1!Ab1!Ab1!"));
    }
}
