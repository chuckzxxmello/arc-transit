package com.transit.arctransit.fleet;

/**
 * Query parameters for searching fleet units.
 */
public record FleetUnitQuery(
        String unitNumberFilter,
        String statusFilter
) {}
