package com.transit.arctransit.fleet.domain;

/**
 * Classification of a fleet vehicle.
 *
 * Arc Transit's current version accepts only BUS.
 * The database enforces this with ck_fleet_vehicle_type.
 *
 * Additional vehicle types (e.g., SUV, SEDAN) can be introduced
 * through a future Flyway migration that updates the check constraint.
 */
public enum VehicleType {
    BUS
}
