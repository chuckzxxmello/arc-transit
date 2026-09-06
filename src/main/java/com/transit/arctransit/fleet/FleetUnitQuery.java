package com.transit.arctransit.fleet;

// search criteria for exisitng records of bus fleet units
public record FleetUnitQuery(
        String unitOrPlateFilter,
        String statusFilter
) {}
