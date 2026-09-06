package com.transit.arctransit.incident.domain;

// operational module affected by the incident
public enum AffectedServiceType {

    // fleet-related incident
    FLEET,

    // driver-related incident
    DRIVER,

    // route-related incident
    ROUTE,

    // dispatch-related incident
    DISPATCH
}