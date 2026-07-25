package com.transit.arctransit.auth;

/**
 * Immutable summary view of a user for list grids and search results.
 */
public record UserSummaryView(
        String username,
        String displayName,
        String accountStatus
) {}
