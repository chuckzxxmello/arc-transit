package com.transit.arctransit.driver;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;
// query parameters for searching drivers
public record DriverQuery(
        @Size(max = 200) @SafeText String employeeNumberFilter,
        @Size(max = 200) @SafeText String statusFilter
) {}
