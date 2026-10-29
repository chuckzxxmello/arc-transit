package com.transit.arctransit.auth;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;
// search for users
public record UserQuery(
        @Size(max = 200) @SafeText String searchKeyword
) {}
