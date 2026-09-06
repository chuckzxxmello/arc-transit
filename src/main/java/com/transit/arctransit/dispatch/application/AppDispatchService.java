package com.transit.arctransit.dispatch.application;

// dispatch module
import com.transit.arctransit.dispatch.CreateDispatchCommand;
import com.transit.arctransit.dispatch.DispatchAssignmentView;
import com.transit.arctransit.dispatch.DispatchQuery;
import com.transit.arctransit.dispatch.DispatchService;

// dispatch domain
import com.transit.arctransit.dispatch.domain.DispatchAssignment;
import com.transit.arctransit.dispatch.domain.DispatchAssignmentRepository;
import com.transit.arctransit.dispatch.domain.DispatchStatus;

// fleet module
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitView;

// driver module
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.driver.DriverView;

// route module
import com.transit.arctransit.route.RouteManagementService;
import com.transit.arctransit.route.RouteView;

// audit
import com.transit.arctransit.audit.AuditRecordingService;

// common exceptions
import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;

// java
import java.util.List;

// spring
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



// dispatch application service
@Service
@Transactional
public class AppDispatchService
        implements DispatchService {

    // constants
    private static final Logger log = LoggerFactory.getLogger(
                    AppDispatchService.class
            );

    private static final String CANCELLED_STATUS = DispatchStatus.CANCELLED.name();
    private static final String ACTIVE_STATUS = "ACTIVE";

    // dependencies
    private final DispatchAssignmentRepository assignmentRepository;
    private final FleetManagementService fleetService;
    private final DriverManagementService driverService;
    private final RouteManagementService routeService;
    private final AuditRecordingService auditService;

    // constructor
    public AppDispatchService(
            DispatchAssignmentRepository assignmentRepository,
            FleetManagementService fleetService,
            DriverManagementService driverService,
            RouteManagementService routeService,
            AuditRecordingService auditService) {

        this.assignmentRepository = assignmentRepository;
        this.fleetService = fleetService;
        this.driverService = driverService;
        this.routeService = routeService;
        this.auditService = auditService;
    }

    // create assignment
    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public DispatchAssignmentView createAssignment(
            CreateDispatchCommand command) {

        FleetUnitView fleetUnit =
                validateFleetUnit(
                        command.fleetUnitId()
                );

        DriverView driver =
                validateDriver(
                        command.driverId()
                );

        RouteView route =
                validateRoute(
                        command.routeId()
                );

        validateFleetScheduleConflict(
                command,
                fleetUnit
        );

        validateDriverScheduleConflict(
                command,
                driver
        );

        DispatchAssignment assignment =
                new DispatchAssignment(
                        command.fleetUnitId(),
                        command.driverId(),
                        command.routeId(),
                        command.dispatchDate(),
                        command.scheduledDeparture(),
                        command.scheduledArrival(),
                        normalizeNotes(
                                command.notes()
                        )
                );

        DispatchAssignment saved =
                assignmentRepository.save(
                        assignment
                );

        auditService.recordAction(
                "DISPATCH_ASSIGNED",
                "DispatchAssignment",
                saved.getId(),
                "Assigned Bus "
                        + command.fleetUnitId()
                        + " to Driver "
                        + command.driverId()
                        + " on Route "
                        + command.routeId()
        );

        log.info(
                "Created dispatch assignment {}.",
                saved.getId()
        );

        return resolveView(saved);
    }

    // validate fleet unit
    private FleetUnitView validateFleetUnit(
            Long fleetUnitId) {

        FleetUnitView fleetUnit =
                fleetService.getUnit(
                        fleetUnitId
                );

        if (!ACTIVE_STATUS.equals(
                fleetUnit.operationalStatus()
        )) {

            throw new CommandValidationException(
                    "Fleet unit "
                            + fleetUnit.unitNumber()
                            + " is not ACTIVE (current: "
                            + fleetUnit.operationalStatus()
                            + ")"
            );
        }

        return fleetUnit;
    }

    // validate driver
    private DriverView validateDriver(
            Long driverId) {

        DriverView driver =
                driverService.getDriver(
                        driverId
                );

        if (!ACTIVE_STATUS.equals(
                driver.employmentStatus()
        )) {

            throw new CommandValidationException(
                    "Driver "
                            + driver.firstName()
                            + " "
                            + driver.lastName()
                            + " is not ACTIVE (current: "
                            + driver.employmentStatus()
                            + ")"
            );
        }

        if (driver.licenseExpired()) {

            throw new CommandValidationException(
                    "Driver "
                            + driver.firstName()
                            + " "
                            + driver.lastName()
                            + " has an expired license (expired: "
                            + driver.licenseExpiryDate()
                            + ")"
            );
        }

        return driver;
    }

    // validate route
    private RouteView validateRoute(
            Long routeId) {

        RouteView route =
                routeService.getRoute(
                        routeId
                );

        if (!ACTIVE_STATUS.equals(
                route.routeStatus()
        )) {

            throw new CommandValidationException(
                    "Route "
                            + route.routeCode()
                            + " is not ACTIVE (current: "
                            + route.routeStatus()
                            + ")"
            );
        }

        return route;
    }

    // validate fleet scheduling conflict
    private void validateFleetScheduleConflict(
            CreateDispatchCommand command,
            FleetUnitView fleetUnit) {

        List<DispatchAssignment> conflicts =
                assignmentRepository
                        .findByFleetUnitIdAndDispatchDateAndDispatchStatusNot(
                                command.fleetUnitId(),
                                command.dispatchDate(),
                                DispatchStatus.CANCELLED
                        );

        if (!conflicts.isEmpty()) {

            throw new BusinessConflictException(
                    "Fleet unit "
                            + fleetUnit.unitNumber()
                            + " already has an assignment on "
                            + command.dispatchDate()
            );
        }
    }

    // validate driver scheduling conflict
    private void validateDriverScheduleConflict(
            CreateDispatchCommand command,
            DriverView driver) {

        List<DispatchAssignment> conflicts =
                assignmentRepository
                        .findByDriverIdAndDispatchDateAndDispatchStatusNot(
                                command.driverId(),
                                command.dispatchDate(),
                                DispatchStatus.CANCELLED
                        );

        if (!conflicts.isEmpty()) {

            throw new BusinessConflictException(
                    "Driver "
                            + driver.firstName()
                            + " "
                            + driver.lastName()
                            + " already has an assignment on "
                            + command.dispatchDate()
            );
        }
    }

    // start trip
    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public DispatchAssignmentView startTrip(
            Long assignmentId) {

        DispatchAssignment assignment =
                findAssignment(assignmentId);

        assignment.startTrip();

        auditService.recordAction(
                "DISPATCH_STARTED",
                "DispatchAssignment",
                assignment.getId(),
                "Started dispatch trip"
        );

        log.info(
                "Started dispatch assignment {}.",
                assignmentId
        );

        return resolveView(assignment);
    }

    // complete trip
    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public DispatchAssignmentView completeTrip(
            Long assignmentId) {

        DispatchAssignment assignment =
                findAssignment(assignmentId);

        assignment.completeTrip();

        auditService.recordAction(
                "DISPATCH_COMPLETED",
                "DispatchAssignment",
                assignment.getId(),
                "Completed dispatch trip"
        );

        log.info(
                "Completed dispatch assignment {}.",
                assignmentId
        );

        return resolveView(assignment);
    }

    // cancel assignment
    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public DispatchAssignmentView cancelAssignment(
            Long assignmentId) {

        DispatchAssignment assignment =
                findAssignment(assignmentId);

        assignment.cancel();

        auditService.recordAction(
                "DISPATCH_CANCELLED",
                "DispatchAssignment",
                assignment.getId(),
                "Cancelled dispatch assignment"
        );

        log.info(
                "Cancelled dispatch assignment {}.",
                assignmentId
        );

        return resolveView(assignment);
    }

    // search active assignments
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public Page<DispatchAssignmentView> searchAssignments(
            DispatchQuery query,
            Pageable pageable) {

        return assignmentRepository
                .findByArchivedAtIsNull(pageable)
                .map(this::resolveView);
    }

    // completed trip count
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public long countCompletedTrips() {

        return assignmentRepository
                .countByDispatchStatusAndArchivedAtIsNull(
                        DispatchStatus.COMPLETED
                );
    }

    // archive assignment
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void archiveAssignment(
            Long id) {

        DispatchAssignment assignment =
                findAssignment(id);

        if (assignment.getDispatchStatus()
                == DispatchStatus.SCHEDULED
                || assignment.getDispatchStatus()
                == DispatchStatus.IN_PROGRESS) {

            throw new CommandValidationException(
                    "Cannot archive an active assignment."
            );
        }

        assignment.archive();

        auditService.recordAction(
                "DISPATCH_ARCHIVED",
                "DispatchAssignment",
                id,
                "Archived dispatch assignment"
        );

        log.info(
                "Archived dispatch assignment {}.",
                id
        );
    }

    // unarchive assignment
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void unarchiveAssignment(
            Long id) {

        DispatchAssignment assignment =
                findAssignment(id);

        assignment.unarchive();

        auditService.recordAction(
                "DISPATCH_UNARCHIVED",
                "DispatchAssignment",
                id,
                "Unarchived dispatch assignment"
        );

        log.info(
                "Unarchived dispatch assignment {}.",
                id
        );
    }

    // search archived assignments
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public Page<DispatchAssignmentView>
    searchArchivedAssignments(
            Pageable pageable) {

        return assignmentRepository
                .findByArchivedAtIsNotNull(pageable)
                .map(this::resolveView);
    }

    // find assignment
    private DispatchAssignment findAssignment(
            Long id) {

        return assignmentRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Dispatch assignment not found: "
                                        + id
                        )
                );
    }

    // resolve cross-module display values
    private DispatchAssignmentView resolveView(
            DispatchAssignment assignment) {

        String fleetUnitNumber =
                resolveFleetUnitNumber(
                        assignment.getFleetUnitId()
                );

        String driverName =
                resolveDriverName(
                        assignment.getDriverId()
                );

        String routeCode =
                resolveRouteCode(
                        assignment.getRouteId()
                );

        return toView(
                assignment,
                fleetUnitNumber,
                driverName,
                routeCode
        );
    }

    // resolve fleet unit display name
    private String resolveFleetUnitNumber(
            Long fleetUnitId) {

        try {

            return fleetService
                    .getUnit(fleetUnitId)
                    .unitNumber();

        } catch (ResourceNotFoundException exception) {

            return "UNKNOWN-" + fleetUnitId;
        }
    }

    // resolve driver display name
    private String resolveDriverName(
            Long driverId) {

        try {

            DriverView driver =
                    driverService.getDriver(
                            driverId
                    );

            return driver.firstName()
                    + " "
                    + driver.lastName();

        } catch (ResourceNotFoundException exception) {

            return "UNKNOWN-" + driverId;
        }
    }

    // resolve route display code
    private String resolveRouteCode(
            Long routeId) {

        try {

            return routeService
                    .getRoute(routeId)
                    .routeCode();

        } catch (ResourceNotFoundException exception) {

            return "UNKNOWN-" + routeId;
        }
    }

    // map entity to view
    private DispatchAssignmentView toView(
            DispatchAssignment assignment,
            String fleetUnitNumber,
            String driverName,
            String routeCode) {

        return new DispatchAssignmentView(
                assignment.getId(),
                assignment.getDispatchDate().toString(),
                assignment.getFleetUnitId(),
                fleetUnitNumber,
                assignment.getDriverId(),
                driverName,
                assignment.getRouteId(),
                routeCode,
                assignment
                        .getScheduledDeparture()
                        .toString(),
                assignment
                        .getDispatchStatus()
                        .name()
        );
    }

    // normalize notes
    private static String normalizeNotes(
            String value) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isBlank()
                ? null
                : normalized;
    }
}