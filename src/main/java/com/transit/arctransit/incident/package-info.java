// incident module

// owns operational incident reports,
// severity, lifecycle state, and resolution workflow

// depends on audit and common

@org.springframework.modulith.ApplicationModule(
        displayName = "Incident",
        allowedDependencies = {
                "common",
                "audit",
                "reporting",
                "fleet",
                "driver",
                "route",
                "dispatch",
                "maintenance"
        }
)
package com.transit.arctransit.incident;