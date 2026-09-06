package com.transit.arctransit;

import org.junit.jupiter.api.Test;

import org.springframework.modulith.core.ApplicationModules;

// verifies Spring Modulith architecture

class ModularityTests {

    @Test
    void verifiesApplicationModuleStructure() {

        ApplicationModules modules =
                ApplicationModules.of(
                        ArcTransitSystemApplication.class
                );

        modules.verify();
    }
}