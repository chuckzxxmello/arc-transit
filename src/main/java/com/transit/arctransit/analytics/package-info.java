/*
Arc Transit Analytics module.
 
 This module owns read-only operational summaries,
 dashboard metrics, and the main dashboard interface.
 */

@org.springframework.modulith.ApplicationModule(
        displayName = "Analytics",
        allowedDependencies = {"auth", "fleet", "driver", "route", "dispatch", "common"}
)
package com.transit.arctransit.analytics;