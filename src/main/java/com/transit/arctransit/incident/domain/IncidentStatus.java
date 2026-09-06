package com.transit.arctransit.incident.domain;

// incident lifecycle
public enum IncidentStatus {

    // newly reported
    REPORTED,

    // reviewed by staff
    ACKNOWLEDGED,

    // investigation is active
    INVESTIGATING,

    // issue has been resolved
    RESOLVED,

    // resolution has been verified
    CLOSED
}