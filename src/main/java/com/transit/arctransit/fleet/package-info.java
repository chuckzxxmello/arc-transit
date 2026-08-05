/*
fleet module.
 
 Owns bus-unit master records and manually submitted
 operational status information.
 */

@org.springframework.modulith.ApplicationModule(
        displayName = "Fleet Management",
        allowedDependencies = {"common", "audit"}
)
package com.transit.arctransit.fleet;