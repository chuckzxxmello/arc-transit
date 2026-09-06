package com.transit.arctransit.analytics.application;

// dispatch
import com.transit.arctransit.dispatch.DispatchService;

// driver
import com.transit.arctransit.driver.DriverManagementService;

// fleet
import com.transit.arctransit.fleet.FleetManagementService;

// route
import com.transit.arctransit.route.RouteManagementService;

// spring
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

// startup diagnostic runner

@Component
public class StartupDiagnosticRunner
        implements ApplicationRunner {

    // logger
    private static final Logger log =
            LoggerFactory.getLogger(
                    StartupDiagnosticRunner.class
            );

    // diagnostic authority
    private static final String SYSTEM_ADMIN_AUTHORITY =
            "ROLE_SYSTEM_ADMIN";

    // dependencies
    private final FleetManagementService fleetService;
    private final DriverManagementService driverService;
    private final RouteManagementService routeService;
    private final DispatchService dispatchService;

    // constructor
    public StartupDiagnosticRunner(
            FleetManagementService fleetService,
            DriverManagementService driverService,
            RouteManagementService routeService,
            DispatchService dispatchService) {

        this.fleetService = fleetService;
        this.driverService = driverService;
        this.routeService = routeService;
        this.dispatchService = dispatchService;
    }

    // run diagnostics after application startup
    @Override
    public void run(
            ApplicationArguments args) {

        log.info(
                "===== ARC TRANSIT STARTUP DIAGNOSTIC ====="
        );

        installDiagnosticAuthentication();

        try {

            verifyFleet();
            verifyDrivers();
            verifyRoutes();
            verifyDispatch();

            log.info(
                    "===== ALL MODULES VERIFIED SUCCESSFULLY ====="
            );

        } catch (Exception exception) {

            log.error(
                    "===== STARTUP DIAGNOSTIC FAILED =====",
                    exception
            );

        } finally {

            SecurityContextHolder.clearContext();
        }
    }

    // install temporary admin authority
    private void installDiagnosticAuthentication() {

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "startup-diagnostic",
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        SYSTEM_ADMIN_AUTHORITY
                                )
                        )
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        authentication
                );
    }

    // verify Fleet module
    private void verifyFleet() {

        long count =
                fleetService
                        .searchUnits(
                                null,
                                PageRequest.of(0, 1)
                        )
                        .getTotalElements();

        log.info(
                "[Fleet]    Active fleet units: {}",
                count
        );
    }

    // verify Driver module
    private void verifyDrivers() {

        long count =
                driverService
                        .searchDrivers(
                                null,
                                PageRequest.of(0, 1)
                        )
                        .getTotalElements();

        log.info(
                "[Driver]   Active drivers: {}",
                count
        );
    }

    // verify Route module
    private void verifyRoutes() {

        long count =
                routeService
                        .searchRoutes(
                                null,
                                PageRequest.of(0, 1)
                        )
                        .getTotalElements();

        log.info(
                "[Route]    Active routes: {}",
                count
        );
    }

    // verify Dispatch module
    private void verifyDispatch() {

        long completedTrips =
                dispatchService
                        .countCompletedTrips();

        long totalAssignments =
                dispatchService
                        .searchAssignments(
                                null,
                                PageRequest.of(0, 1)
                        )
                        .getTotalElements();

        log.info(
                "[Dispatch] Completed trips: {}",
                completedTrips
        );

        log.info(
                "[Dispatch] Total assignments: {}",
                totalAssignments
        );
    }
}