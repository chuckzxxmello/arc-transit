package com.transit.arctransit.dispatch.domain;

// dispatch assignment lifecycle state
public enum DispatchStatus {

    // scheduled but not started
    SCHEDULED,

    // trip is currently in progress
    IN_PROGRESS,

    // trip completed successfully
    COMPLETED,

    // trip cancelled
    CANCELLED
}