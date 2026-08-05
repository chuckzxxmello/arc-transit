package com.transit.arctransit.analytics.application;

import com.transit.arctransit.dispatch.DispatchService;
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.route.RouteManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Startup diagnostic runner that verifies all module APIs are accessible
 * and logs summary statistics to the console.
 *
 * This runs on every application startup to provide runtime proof that
 * the Fleet, Driver, Route, and Dispatch service layers are working.
 *
 * Uses a temporary SecurityContext with SYSTEM_ADMIN authority so that
 * the @PreAuthorize checks pass during the verification queries.
 *
 * Source: https://docs.spring.io/spring-boot/api/java/org/springframework/boot/ApplicationRunner.html
 */
@Component
public class StartupDiagnosticRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupDiagnosticRunner.class);

    private final FleetManagementService fleetService;
    private final DriverManagementService driverService;
    private final RouteManagementService routeService;
    private final DispatchService dispatchService;

    public StartupDiagnosticRunner(FleetManagementService fleetService,
                                    DriverManagementService driverService,
                                    RouteManagementService routeService,
                                    DispatchService dispatchService) {
        this.fleetService = fleetService;
        this.driverService = driverService;
        this.routeService = routeService;
        this.dispatchService = dispatchService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("===== ARC TRANSIT STARTUP DIAGNOSTIC =====");

        // Set up a temporary SYSTEM_ADMIN context for @PreAuthorize checks
        var auth = new UsernamePasswordAuthenticationToken(
                "startup-diagnostic", null,
                List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            long fleetCount = fleetService.searchUnits(null, PageRequest.of(0, 1)).getTotalElements();
            log.info("[Fleet]    Active fleet units: {}", fleetCount);

            long driverCount = driverService.searchDrivers(null, PageRequest.of(0, 1)).getTotalElements();
            log.info("[Driver]   Active drivers: {}", driverCount);

            long routeCount = routeService.searchRoutes(null, PageRequest.of(0, 1)).getTotalElements();
            log.info("[Route]    Active routes: {}", routeCount);

            long completedTrips = dispatchService.countCompletedTrips();
            log.info("[Dispatch] Completed trips: {}", completedTrips);

            long totalAssignments = dispatchService.searchAssignments(null, PageRequest.of(0, 1)).getTotalElements();
            log.info("[Dispatch] Total assignments: {}", totalAssignments);

            log.info("===== ALL MODULES VERIFIED SUCCESSFULLY =====");
        } catch (Exception e) {
            log.error("===== STARTUP DIAGNOSTIC FAILED =====", e);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
