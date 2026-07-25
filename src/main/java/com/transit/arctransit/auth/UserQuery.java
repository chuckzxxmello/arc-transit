package com.transit.arctransit.auth;

/**
 * Search criteria for querying users.
 */
public record UserQuery(
        String searchKeyword
) {}
