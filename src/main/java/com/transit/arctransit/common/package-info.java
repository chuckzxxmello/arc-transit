/*
 * Common shared library module.
 * 
 * Contains cross-module utilities and standard exceptions.
 * Exposed as an OPEN module so its subpackages (like exception)
 * are accessible to all other business modules.
 */

@org.springframework.modulith.ApplicationModule(
    displayName = "Common Shared Library",
    type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.transit.arctransit.common;
