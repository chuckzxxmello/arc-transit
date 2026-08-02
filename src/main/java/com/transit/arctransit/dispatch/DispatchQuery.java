package com.transit.arctransit.dispatch;

/**
 * Query parameters for searching dispatch assignments.
 */
public record DispatchQuery(
        String dateFilter,
        String statusFilter
) {}
