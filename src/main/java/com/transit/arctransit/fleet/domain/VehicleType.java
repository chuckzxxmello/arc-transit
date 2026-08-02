package com.transit.arctransit.fleet.domain;

/**
 * Classification of a fleet vehicle.
 *
 * Arc Transit Version 1 accepts only BUS.
 * The database enforces this with ck_fleet_vehicle_type.
 *
 * Additional vehicle types (e.g., MINIBUS, VAN) can be introduced
 * through a future Flyway migration that updates the check constraint.
 */
public enum VehicleType {
    BUS
}
