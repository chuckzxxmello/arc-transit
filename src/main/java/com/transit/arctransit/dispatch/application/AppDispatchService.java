package com.transit.arctransit.dispatch.application;

import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;
import com.transit.arctransit.dispatch.*;
import com.transit.arctransit.dispatch.domain.DispatchAssignment;
import com.transit.arctransit.dispatch.domain.DispatchAssignmentRepository;
import com.transit.arctransit.dispatch.domain.DispatchStatus;
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.driver.DriverView;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitView;
import com.transit.arctransit.route.RouteManagementService;
import com.transit.arctransit.route.RouteView;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Internal implementation of DispatchService.
 *
 * Cross-module validation pattern:
 * This service calls the public service interfaces of Fleet, Driver,
 * and Route modules to validate that referenced entities exist and
 * meet business rules. It does NOT directly access their internal
 * JPA repositories, respecting Spring Modulith module boundaries.
 *
 * Source: https://docs.spring.io/spring-modulith/reference/fundamentals.html
 * (Ctrl+F: "allowedDependencies")
 *
 * Overlap detection:
 * Uses the DispatchAssignmentRepository to find existing non-cancelled
 * assignments for the same bus or driver on the same date. If any
 * overlap is found, a BusinessConflictException is thrown.
 *
 * The database also enforces this through partial unique indexes
 * (ux_dispatch_fleet_unit_schedule, ux_dispatch_driver_schedule)
 * as a final safety boundary.
 * Source: https://www.postgresql.org/docs/17/indexes-partial.html
 */
@Service
@Transactional
public class AppDispatchService implements DispatchService {

    private final DispatchAssignmentRepository assignmentRepository;
    private final FleetManagementService fleetService;
    private final DriverManagementService driverService;
    private final RouteManagementService routeService;

    /**
     * Constructor injection of cross-module service interfaces.
     *
     * Spring Modulith allows this because dispatch/package-info.java
     * declares allowedDependencies = {"fleet", "driver", "route", "common"}.
     */
    public AppDispatchService(DispatchAssignmentRepository assignmentRepository,
            FleetManagementService fleetService,
            DriverManagementService driverService,
            RouteManagementService routeService) {
        this.assignmentRepository = assignmentRepository;
        this.fleetService = fleetService;
        this.driverService = driverService;
        this.routeService = routeService;
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public DispatchAssignmentView createAssignment(@Valid CreateDispatchCommand command) {
        /*
         * Step 1: Validate fleet unit exists and is ACTIVE.
         */
        FleetUnitView fleetUnit = fleetService.getUnit(command.fleetUnitId());
        if (!"ACTIVE".equals(fleetUnit.operationalStatus())) {
            throw new CommandValidationException(
                    "Fleet unit " + fleetUnit.unitNumber() + " is not ACTIVE (current: " +
                            fleetUnit.operationalStatus() + ")");
        }

        /*
         * Step 2: Validate driver exists, is ACTIVE, and has a valid license.
         */
        DriverView driver = driverService.getDriver(command.driverId());
        if (!"ACTIVE".equals(driver.employmentStatus())) {
            throw new CommandValidationException(
                    "Driver " + driver.firstName() + " " + driver.lastName() +
                            " is not ACTIVE (current: " + driver.employmentStatus() + ")");
        }
        if (driver.licenseExpired()) {
            throw new CommandValidationException(
                    "Driver " + driver.firstName() + " " + driver.lastName() +
                            " has an expired license (expired: " + driver.licenseExpiryDate() + ")");
        }

        /*
         * Step 3: Validate route exists and is ACTIVE.
         */
        RouteView route = routeService.getRoute(command.routeId());
        if (!"ACTIVE".equals(route.routeStatus())) {
            throw new CommandValidationException(
                    "Route " + route.routeCode() + " is not ACTIVE (current: " +
                            route.routeStatus() + ")");
        }

        /*
         * Step 4: Check for overlapping bus assignments on the same date.
         */
        List<DispatchAssignment> busConflicts = assignmentRepository
                .findByFleetUnitIdAndDispatchDateAndDispatchStatusNot(
                        command.fleetUnitId(), command.dispatchDate(), DispatchStatus.CANCELLED);
        if (!busConflicts.isEmpty()) {
            throw new BusinessConflictException(
                    "Fleet unit " + fleetUnit.unitNumber() +
                            " already has an assignment on " + command.dispatchDate());
        }

        /*
         * Step 5: Check for overlapping driver assignments on the same date.
         */
        List<DispatchAssignment> driverConflicts = assignmentRepository
                .findByDriverIdAndDispatchDateAndDispatchStatusNot(
                        command.driverId(), command.dispatchDate(), DispatchStatus.CANCELLED);
        if (!driverConflicts.isEmpty()) {
            throw new BusinessConflictException(
                    "Driver " + driver.firstName() + " " + driver.lastName() +
                            " already has an assignment on " + command.dispatchDate());
        }

        /*
         * Step 6: Create and save the assignment.
         */
        DispatchAssignment assignment = new DispatchAssignment(
                command.fleetUnitId(),
                command.driverId(),
                command.routeId(),
                command.dispatchDate(),
                command.scheduledDeparture(),
                command.scheduledArrival(),
                command.notes());

        assignmentRepository.saveAndFlush(assignment);
        return toView(assignment, fleetUnit.unitNumber(),
                driver.firstName() + " " + driver.lastName(), route.routeCode());
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public DispatchAssignmentView startTrip(Long assignmentId) {
        DispatchAssignment assignment = findAssignment(assignmentId);
        assignment.startTrip();
        return resolveView(assignment);
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public DispatchAssignmentView completeTrip(Long assignmentId) {
        DispatchAssignment assignment = findAssignment(assignmentId);
        assignment.completeTrip();
        return resolveView(assignment);
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public DispatchAssignmentView cancelAssignment(Long assignmentId) {
        DispatchAssignment assignment = findAssignment(assignmentId);
        assignment.cancel();
        return resolveView(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public Page<DispatchAssignmentView> searchAssignments(DispatchQuery query, Pageable pageable) {
        return assignmentRepository.findAll(pageable)
                .map(this::resolveView);
    }

    // --- Helpers ---

    private DispatchAssignment findAssignment(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch assignment not found: " + id));
    }

    /**
     * Resolves cross-module display names by calling each module's public service.
     */
    private DispatchAssignmentView resolveView(DispatchAssignment assignment) {
        String unitNumber;
        try {
            unitNumber = fleetService.getUnit(assignment.getFleetUnitId()).unitNumber();
        } catch (ResourceNotFoundException e) {
            unitNumber = "UNKNOWN-" + assignment.getFleetUnitId();
        }

        String driverName;
        try {
            DriverView driver = driverService.getDriver(assignment.getDriverId());
            driverName = driver.firstName() + " " + driver.lastName();
        } catch (ResourceNotFoundException e) {
            driverName = "UNKNOWN-" + assignment.getDriverId();
        }

        String routeCode;
        try {
            routeCode = routeService.getRoute(assignment.getRouteId()).routeCode();
        } catch (ResourceNotFoundException e) {
            routeCode = "UNKNOWN-" + assignment.getRouteId();
        }

        return toView(assignment, unitNumber, driverName, routeCode);
    }

    private DispatchAssignmentView toView(DispatchAssignment assignment,
            String unitNumber, String driverName, String routeCode) {
        return new DispatchAssignmentView(
                assignment.getId(),
                assignment.getDispatchDate().toString(),
                assignment.getFleetUnitId(),
                unitNumber,
                assignment.getDriverId(),
                driverName,
                assignment.getRouteId(),
                routeCode,
                assignment.getScheduledDeparture().toString(),
                assignment.getDispatchStatus().name());
    }
}
