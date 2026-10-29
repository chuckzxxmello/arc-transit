package com.transit.arctransit.dispatch;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;
// search criteria for dispatch assignments
public record DispatchQuery(

        @Size(max = 200) @SafeText String dateFilter,
        @Size(max = 200) @SafeText String statusFilter

) {
}