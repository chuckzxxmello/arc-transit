package com.transit.arctransit.fleet.domain;

/**
 * Operational availability state of a fleet unit.
 */
public enum OperationalStatus {

    /** The bus is available for dispatch. */
    ACTIVE,

    /** The bus is registered but not currently available. */
    INACTIVE,

    /** The bus is undergoing scheduled or unscheduled maintenance. */
    UNDER_MAINTENANCE,

    /** The bus is permanently or indefinitely removed from service. */
    OUT_OF_SERVICE
}
