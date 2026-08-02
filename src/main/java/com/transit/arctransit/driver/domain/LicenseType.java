package com.transit.arctransit.driver.domain;

/**
 * Classification of a driver's license.
 *
 * Matches the database check constraint ck_drivers_license_type
 * in V3__create_drivers.sql.
 */
public enum LicenseType {

    /** Licensed to operate public utility vehicles (buses). */
    PROFESSIONAL,

    /** Standard personal license. */
    NON_PROFESSIONAL
}
