package com.transit.arctransit.route;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;
// search criteria for routes
public record RouteQuery(

        @Size(max = 200) @SafeText String routeOrNameFilter,
        @Size(max = 200) @SafeText String statusFilter

) {
}