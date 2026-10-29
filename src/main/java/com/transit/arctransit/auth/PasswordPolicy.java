package com.transit.arctransit.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PasswordPolicy {
    public static final int MAX_LENGTH = 16;
    public static final String PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9])[\\x21-\\x7E]{8,16}$";
    public static final String MESSAGE =
            "Password must be at least 8 characters long. Include uppercase, lowercase, a number and a special character. " +
            "Standard punctuation such as #, @ and ! is allowed. Do not use spaces.";

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null && password.matches(PATTERN);
    }

    public static String validationMessage(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required.";
        }
        if (password.length() < 8) {
            return "Password must be at least 8 characters long.";
        }
        if (password.length() > MAX_LENGTH) {
            return "Password must not exceed 16 characters.";
        }
        if (!password.matches("[\\x21-\\x7E]+")) {
            return "Use standard letters, numbers and symbols without spaces.";
        }
        List<String> missing = new ArrayList<>();
        if (!password.matches(".*[A-Z].*")) {
            missing.add("an uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            missing.add("a lowercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            missing.add("a number");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            missing.add("a special character");
        }
        return missing.isEmpty() ? null : "Password needs " + String.join(", ", missing) + ".";
    }

    public static String estimatedStrength(String password) {
        if (!isValid(password)) {
            return "weak";
        }
        String lower = password.toLowerCase(Locale.ROOT);
        if (List.of("password", "qwerty", "letmein", "welcome", "arctransit")
                .stream().anyMatch(lower::contains)
                || password.chars().distinct().count() < 6
                || password.matches("(.{1,8})\\1{2,}")) {
            return "weak";
        }
        if (password.length() >= 16) {
            return "very strong";
        }
        return password.length() >= 12 ? "strong" : "moderate";
    }
}
