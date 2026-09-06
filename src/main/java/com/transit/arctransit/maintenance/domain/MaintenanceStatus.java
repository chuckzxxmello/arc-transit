package com.transit.arctransit.maintenance.domain;

// maintenance lifecycle
public enum MaintenanceStatus {

    // request created
    REQUESTED,

    // request approved
    APPROVED,

    // work currently underway
    IN_PROGRESS,

    // work completed
    COMPLETED,

    // request or work cancelled
    CANCELLED
}