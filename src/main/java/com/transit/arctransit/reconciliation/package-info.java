/*
reconciliation module.

Owns the operational exception queue, contradiction detection,
and stale-state detection logic.

This module is the overarching observer of the system, verifying
state across all other operational modules.

Source: Spring Modulith — https://docs.spring.io/spring-modulith/reference/fundamentals.html
 */

@org.springframework.modulith.ApplicationModule(
        displayName = "Reconciliation",
        allowedDependencies = {"fleet", "driver", "route", "dispatch", "incident", "maintenance", "audit", "common", "reporting"}
)
package com.transit.arctransit.reconciliation;
