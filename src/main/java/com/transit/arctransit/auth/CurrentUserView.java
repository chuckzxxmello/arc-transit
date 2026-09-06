package com.transit.arctransit.auth;

import java.util.Set;

// view current authenticated user's session.
public record CurrentUserView(
        String username,
        Set<String> roles
) {}
