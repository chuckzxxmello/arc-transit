package com.transit.arctransit.driver.domain;

/**
 * Employment lifecycle state of a driver.
 *
 * Matches the database check constraint ck_drivers_employment_status
 * in V3__create_drivers.sql.
 */
public enum EmploymentStatus {

    /** The driver is available for dispatch assignments. */
    ACTIVE,

    /** The driver is registered but not currently available. */
    INACTIVE,

    /** The driver is temporarily barred from assignments. */
    SUSPENDED,

    /** The driver's employment has ended permanently. */
    TERMINATED
}
