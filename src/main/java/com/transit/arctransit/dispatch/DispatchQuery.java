package com.transit.arctransit.dispatch;

// search criteria for dispatch assignments
public record DispatchQuery(

        String dateFilter,
        String statusFilter

) {
}