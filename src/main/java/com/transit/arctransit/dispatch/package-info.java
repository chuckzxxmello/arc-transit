/*
dispatch module.

owns dated vehicle, driver, route, and schedule
assignments and their operational lifecycle.

This module is allowed to depend on fleet, driver, route, and common
because it must validate that referenced entities exist and meet
business rules (e.g., active status, valid license) before creating
dispatch assignments.

Spring Modulith enforces these boundaries through ModularityTests.
Source: https://docs.spring.io/spring-modulith/reference/fundamentals.html
(Ctrl+F: allowedDependencies)
 */

@org.springframework.modulith.ApplicationModule(
        displayName = "Dispatch",
        allowedDependencies = {"fleet", "driver", "route", "common", "audit"}
)
package com.transit.arctransit.dispatch;