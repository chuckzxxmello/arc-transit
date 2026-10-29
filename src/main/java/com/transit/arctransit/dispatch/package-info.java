// dispatch module

// owns dated vehicle, driver, route, and schedule assignments
// and their operational lifecycle.

// dispatch may depend on fleet, driver, route, common, and audit
// because it validates cross-module operational rules.

@org.springframework.modulith.ApplicationModule(
        displayName = "Dispatch",
        allowedDependencies = {
                "fleet",
                "driver",
                "route",
                "maintenance",
                "common",
                "audit"
        }
)
package com.transit.arctransit.dispatch;
