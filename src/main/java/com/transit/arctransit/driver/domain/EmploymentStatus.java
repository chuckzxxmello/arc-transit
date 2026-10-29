package com.transit.arctransit.driver.domain;

// employment status of driver
public enum EmploymentStatus {

    // active personnel record; availability and qualification are checked separately
    ACTIVE,

    // administratively inactive personnel record
    INACTIVE,

    // driver is temporarily barred from assignments
    SUSPENDED,

    // driver's employment has ended permanently
    TERMINATED
}
