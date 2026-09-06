// common shared library module

// contains cross-module exceptions and shared UI components

// this module is OPEN so other business modules can access
// its subpackages

@org.springframework.modulith.ApplicationModule(
        displayName = "Common Shared Library",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.transit.arctransit.common;