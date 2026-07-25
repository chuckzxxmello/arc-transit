package com.transit.arctransit.auth;

import java.util.Set;

/**
 * Immutable view of an application user for frontend consumption.
 * Excludes sensitive fields like password hashes.
 */
public record UserView(
        String username,
        String displayName,
        String email,
        String accountStatus,
        Set<String> roles
) {}
