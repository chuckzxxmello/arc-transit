package com.transit.arctransit.driver.domain;

// employment status of driver
public enum EmploymentStatus {

    // driver is available for dispatch assignments
    ACTIVE,

    // driver is registered but not currently available
    INACTIVE,

    // driver is temporarily barred from assignments
    SUSPENDED,

    // driver's employment has ended permanently
    TERMINATED
}
