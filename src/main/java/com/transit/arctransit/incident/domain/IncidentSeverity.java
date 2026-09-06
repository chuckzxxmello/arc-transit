package com.transit.arctransit.incident.domain;

// severity classification for an incidents
public enum IncidentSeverity {

    // minor operational impact
    LOW,

    // noticeable issue requiring attention
    MEDIUM,

    // significant operational impact
    HIGH,

    // critical service disruption
    CRITICAL
}