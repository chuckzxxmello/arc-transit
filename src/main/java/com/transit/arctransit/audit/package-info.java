// audit module

// owns append-only records of important business
// changes and authorization outcomes.

@org.springframework.modulith.ApplicationModule(
        displayName = "Audit",
        allowedDependencies = {"common"}
)
package com.transit.arctransit.audit;