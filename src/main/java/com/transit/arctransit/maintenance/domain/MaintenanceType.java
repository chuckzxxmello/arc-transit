package com.transit.arctransit.maintenance.domain;

// classification of maintenance work
public enum MaintenanceType {

    // planned maintenance
    SCHEDULED,

    // repair caused by an identified problem
    CORRECTIVE,

    // urgent maintenance
    EMERGENCY
}