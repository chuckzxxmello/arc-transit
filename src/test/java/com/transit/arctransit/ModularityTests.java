package com.transit.arctransit;

import com.transit.arctransit.ArcTransitSystemApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Verifies that Arc Transit's package structure follows
 * the declared Spring Modulith boundaries.
 */

class ModularityTests {

    @Test
    void verifiesApplicationModuleStructure() {

        /*
         * Builds Spring Modulith's in-memory representation
         * of the application modules underneath the main package.
         */
        ApplicationModules modules = ApplicationModules.of(ArcTransitSystemApplication.class);

        /*
         * Prints the detected modules during the test.
         *
         * This is useful while the project is still being structured
         * because we can confirm exactly what Modulith recognizes.
         */
        modules.forEach(System.out::println);

        /*
         * Fails the test when Modulith finds architecture violations,
         * such as module cycles or access to another module's internals.
         */
        modules.verify();
    }
}
