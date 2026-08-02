package com.transit.arctransit.fleet.domain;

/**
 * Operational availability state of a fleet unit.
 *
 * Matches the database check constraint ck_fleet_operational_status
 * in V1__create_fleet_units.sql.
 *
 * Source: Jakarta Persistence @Enumerated(EnumType.STRING) maps these
 * enum constants directly to the VARCHAR column values.
 * https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a14935
 * (Ctrl+F: EnumType.STRING)
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
