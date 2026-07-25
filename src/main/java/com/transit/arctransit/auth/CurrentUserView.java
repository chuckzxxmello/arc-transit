package com.transit.arctransit.auth;

import java.util.Set;

/**
 * Immutable view of the currently authenticated user's session.
 */
public record CurrentUserView(
        String username,
        Set<String> roles
) {}
