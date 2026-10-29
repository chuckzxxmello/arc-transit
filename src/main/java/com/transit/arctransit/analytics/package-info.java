// Arc Transit Analytics module.
 
// operational summaries, dashboard metrics
// and the main dashboard interface

@org.springframework.modulith.ApplicationModule(
        displayName = "Analytics",
        allowedDependencies = {"auth", "fleet", "driver", "route", "dispatch", "incident", "maintenance", "reconciliation", "common", "reporting"}
)
package com.transit.arctransit.analytics;