/*
driver module.
 
Owns driver identity, license, employment,
and availability information.

 */

@org.springframework.modulith.ApplicationModule(
        displayName = "Driver Management",
        allowedDependencies = {"common", "audit"}
)
package com.transit.arctransit.driver;