// maintenance module

// owns scheduled and corrective maintenance work
// depends on Fleet, Audit, and Common

@org.springframework.modulith.ApplicationModule(
        displayName = "Maintenance",
        allowedDependencies = {
                "fleet",
                "audit",
                "common"
        }
)
package com.transit.arctransit.maintenance;