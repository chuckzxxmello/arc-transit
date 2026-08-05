/*
route module.
 
owns fixed routes, ordered stops, and recurring
route-schedule templates.

 */

@org.springframework.modulith.ApplicationModule(
        displayName = "Route Management",
        allowedDependencies = {"common", "audit"}
)
package com.transit.arctransit.route;