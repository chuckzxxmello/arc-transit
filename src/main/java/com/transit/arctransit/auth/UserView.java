package com.transit.arctransit.auth;

import java.util.Set;

// application-user details exposed to the user interface
public record UserView(
        String username,
        String displayName,
        String email,
        String accountStatus,
        Set<String> roles
) {}