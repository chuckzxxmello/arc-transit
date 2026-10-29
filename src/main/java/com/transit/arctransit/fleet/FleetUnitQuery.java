package com.transit.arctransit.fleet;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;
// search criteria for exisitng records of bus fleet units
public record FleetUnitQuery(
        @Size(max = 200) @SafeText String unitOrPlateFilter,
        @Size(max = 200) @SafeText String statusFilter
) {}
