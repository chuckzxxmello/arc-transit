package com.transit.arctransit.auth;

// summary view of user to list grids and search results
public record UserSummaryView(
        String username,
        String displayName,
        String accountStatus
) {}
