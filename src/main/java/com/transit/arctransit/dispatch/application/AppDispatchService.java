package com.transit.arctransit.dispatch.application;

// dispatch module
import com.transit.arctransit.dispatch.CreateDispatchCommand;
import com.transit.arctransit.dispatch.DispatchAssignmentView;
import com.transit.arctransit.dispatch.DispatchQuery;
import com.transit.arctransit.dispatch.DispatchService;
import com.transit.arctransit.dispatch.DispatchRule;
import com.transit.arctransit.dispatch.DispatchValidationException;
import com.transit.arctransit.dispatch.DispatchConflictException;
import com.transit.arctransit.dispatch.DispatchEventView;
import com.transit.arctransit.dispatch.domain.DispatchEvent;
import com.transit.arctransit.dispatch.domain.DispatchEventRepository;
import com.transit.arctransit.route.RouteVersionView;
import com.transit.arctransit.maintenance.MaintenanceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

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
@Validated
@Transactional
public class AppDispatchService
        implements DispatchService {

    // constants
    private static final Logger log = LoggerFactory.getLogger(
                    AppDispatchService.class
            );

    private static final String ACTIVE_STATUS = "ACTIVE";

    // dependencies
    private final DispatchAssignmentRepository assignmentRepository;
    private final FleetManagementService fleetService;
    private final DriverManagementService driverService;
    private final RouteManagementService routeService;
    private final AuditRecordingService auditService;
    private final MaintenanceService maintenanceService;
    private final DispatchEventRepository eventRepository;

    @Value("${arc.service-zone:Asia/Manila}")
    private String serviceZone = "Asia/Manila";

    // constructor
    public AppDispatchService(
            DispatchAssignmentRepository assignmentRepository,
            FleetManagementService fleetService,
            DriverManagementService driverService,
            RouteManagementService routeService,
            AuditRecordingService auditService,
            MaintenanceService maintenanceService,
            DispatchEventRepository eventRepository) {

        this.assignmentRepository = assignmentRepository;
        this.fleetService = fleetService;
        this.driverService = driverService;
        this.routeService = routeService;
        this.auditService = auditService;
        this.maintenanceService = maintenanceService;
        this.eventRepository = eventRepository;
    }

    // create assignment
    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public DispatchAssignmentView createAssignment(
            CreateDispatchCommand command) {

        validateCommand(command);
        FleetUnitView fleetUnit = validateFleetUnit(command.fleetUnitId());
        DriverView driver = validateDriver(command.driverId(), command.dispatchDate());
        validateRoute(command.routeId());
        if (command.scheduleId() != null) {
            var schedule = routeService.getSchedule(command.scheduleId());
            if (!schedule.active() || !schedule.routeId().equals(command.routeId())
                    || !schedule.isEffectiveOn(command.dispatchDate())
                    || !schedule.plannedDeparture().equals(command.scheduledDeparture()
                            .atZone(ZoneId.of(serviceZone)).toLocalTime())) {
                throw new DispatchValidationException(DispatchRule.SCHEDULE_NOT_APPLICABLE,
                        "Configured schedule must be active and match the route, date and departure.");
            }
        }

        RouteVersionView version;
        try {
            version = routeService.getEffectiveVersion(command.routeId(), command.dispatchDate());
        } catch (CommandValidationException exception) {
            throw new DispatchValidationException(DispatchRule.ROUTE_VERSION_NOT_EFFECTIVE,
                    exception.getMessage());
        }

        Instant arrival = command.scheduledArrival();
        if (arrival == null && version.estimatedDurationMinutes() != null) {
            arrival = command.scheduledDeparture().plusSeconds(version.estimatedDurationMinutes() * 60L);
        }
        if (arrival == null || !arrival.isAfter(command.scheduledDeparture())) {
            throw new DispatchValidationException(DispatchRule.INVALID_ASSIGNMENT_WINDOW,
                    "Record an arrival after departure or configure a positive route duration.");
        }

        validateOverlaps(command.fleetUnitId(), command.driverId(),
                command.scheduledDeparture(), arrival, null);

        DispatchAssignment assignment = new DispatchAssignment(command.fleetUnitId(),
                command.driverId(), command.routeId(), command.dispatchDate(),
                command.scheduledDeparture(), arrival, normalizeNotes(command.notes()));
        assignment.linkRouteVersion(version.id());
        assignment.linkSchedule(command.scheduleId());

        DispatchAssignment saved = assignmentRepository.save(assignment);
        recordEvent(saved, null);
        auditService.recordAction("DISPATCH_ASSIGNED", "DispatchAssignment", saved.getId(),
                "Assigned fleet unit " + fleetUnit.unitNumber() + " with driver " + driver.employeeNumber());
        log.info("Created dispatch assignment {}.", saved.getId());
        return resolveView(saved);
    }

    private void validateCommand(CreateDispatchCommand command) {
        if (command == null || command.fleetUnitId() == null || command.driverId() == null
                || command.routeId() == null || command.dispatchDate() == null
                || command.scheduledDeparture() == null) {
            throw new DispatchValidationException(DispatchRule.INVALID_ASSIGNMENT_WINDOW,
                    "Vehicle, driver, route, service date and departure are required.");
        }
        if (!command.dispatchDate().equals(command.scheduledDeparture()
                .atZone(ZoneId.of(serviceZone)).toLocalDate())) {
            throw new DispatchValidationException(DispatchRule.INVALID_ASSIGNMENT_WINDOW,
                    "Departure must fall on the service date in " + serviceZone + ".");
        }
        if (command.scheduledArrival() != null
                && !command.scheduledArrival().isAfter(command.scheduledDeparture())) {
            throw new DispatchValidationException(DispatchRule.INVALID_ASSIGNMENT_WINDOW,
                    "Arrival must be after departure.");
        }
    }

    private FleetUnitView validateFleetUnit(Long fleetUnitId) {
        FleetUnitView fleetUnit = fleetService.lockUnitForAssignment(fleetUnitId);
        if (fleetUnit.archivedAt() != null || !ACTIVE_STATUS.equals(fleetUnit.administrativeStatus())) {
            throw new DispatchValidationException(DispatchRule.VEHICLE_UNAVAILABLE,
                    "Fleet unit " + fleetUnit.unitNumber() + " is inactive or archived.");
        }
        if ("INSPECTION_HOLD".equals(fleetUnit.availability())) {
            throw new DispatchValidationException(DispatchRule.VEHICLE_INSPECTION_HOLD,
                    "Fleet unit has an inspection hold.");
        }
        if ("MAINTENANCE".equals(fleetUnit.availability())
                || maintenanceService.countActiveMaintenanceForUnit(fleetUnitId) > 0) {
            throw new DispatchValidationException(DispatchRule.VEHICLE_MAINTENANCE_HOLD,
                    "Fleet unit has an active maintenance condition.");
        }
        if (!"AVAILABLE".equals(fleetUnit.availability())) {
            throw new DispatchValidationException(DispatchRule.VEHICLE_UNAVAILABLE,
                    "Fleet unit availability is " + fleetUnit.availability() + ".");
        }
        return fleetUnit;
    }

    private DriverView validateDriver(Long driverId, LocalDate serviceDate) {
        DriverView driver = driverService.lockDriverForAssignment(driverId);
        if (driver.archivedAt() != null || !ACTIVE_STATUS.equals(driver.employmentStatus())
                || !"AVAILABLE".equals(driver.availability())) {
            throw new DispatchValidationException(DispatchRule.DRIVER_UNAVAILABLE,
                    "Driver is inactive, archived or unavailable.");
        }
        if (!"PROFESSIONAL".equals(driver.licenseType())) {
            throw new DispatchValidationException(DispatchRule.DRIVER_QUALIFICATION_REQUIRED,
                    "Bus dispatch requires a recorded professional driver license.");
        }
        if (driver.licenseExpiryDate() == null
                || LocalDate.parse(driver.licenseExpiryDate()).isBefore(serviceDate)) {
            throw new DispatchValidationException(DispatchRule.DRIVER_LICENSE_EXPIRED,
                    "Required driver license is not valid on " + serviceDate + ".");
        }
        return driver;
    }

    private RouteView validateRoute(Long routeId) {
        RouteView route = routeService.lockRouteForAssignment(routeId);
        if (route.archivedAt() != null || !ACTIVE_STATUS.equals(route.routeStatus())) {
            throw new DispatchValidationException(DispatchRule.ROUTE_VERSION_NOT_EFFECTIVE,
                    "Route is inactive or archived.");
        }
        return route;
    }

    private void validateOverlaps(Long fleetUnitId, Long driverId, Instant start,
                                  Instant end, Long excludeId) {
        if (!assignmentRepository.findFleetOverlaps(fleetUnitId, start, end, excludeId).isEmpty()) {
            throw new DispatchConflictException(DispatchRule.VEHICLE_ASSIGNMENT_OVERLAP,
                    "Fleet unit already has an overlapping assignment.");
        }
        if (!assignmentRepository.findDriverOverlaps(driverId, start, end, excludeId).isEmpty()) {
            throw new DispatchConflictException(DispatchRule.DRIVER_ASSIGNMENT_OVERLAP,
                    "Driver already has an overlapping assignment.");
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

        requireNotArchived(assignment);
        validateFleetUnit(assignment.getFleetUnitId());
        LocalDate startDate = LocalDate.now(ZoneId.of(serviceZone));
        validateDriver(assignment.getDriverId(), startDate.isAfter(assignment.getDispatchDate())
                ? startDate : assignment.getDispatchDate());
        validateRoute(assignment.getRouteId());
        RouteVersionView version = routeService.getVersion(assignment.getRouteVersionId());
        if (!version.isEffectiveOn(assignment.getDispatchDate())) {
            throw new DispatchValidationException(DispatchRule.ROUTE_VERSION_NOT_EFFECTIVE,
                    "The assigned route version does not apply on the service date.");
        }
        if (assignmentRepository.existsByFleetUnitIdAndDispatchStatusAndIdNot(
                assignment.getFleetUnitId(), DispatchStatus.IN_PROGRESS, assignment.getId())) {
            throw new DispatchConflictException(DispatchRule.VEHICLE_ASSIGNMENT_OVERLAP,
                    "Fleet unit has another trip recorded in progress.");
        }
        if (assignmentRepository.existsByDriverIdAndDispatchStatusAndIdNot(
                assignment.getDriverId(), DispatchStatus.IN_PROGRESS, assignment.getId())) {
            throw new DispatchConflictException(DispatchRule.DRIVER_ASSIGNMENT_OVERLAP,
                    "Driver has another trip recorded in progress.");
        }
        validateOverlaps(assignment.getFleetUnitId(), assignment.getDriverId(),
                assignment.getScheduledDeparture(), assignment.getScheduledArrival() == null
                        ? Instant.parse("9999-12-31T23:59:59Z") : assignment.getScheduledArrival(),
                assignment.getId());

        assignment.startTrip();
        recordEvent(assignment, DispatchStatus.SCHEDULED);

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

        requireNotArchived(assignment);
        assignment.completeTrip();
        recordEvent(assignment, DispatchStatus.IN_PROGRESS);

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

        requireNotArchived(assignment);
        DispatchStatus previousStatus = assignment.getDispatchStatus();
        assignment.cancel();
        recordEvent(assignment, previousStatus);

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

        LocalDate date = null;
        DispatchStatus status = null;
        try {
            if (query != null && query.dateFilter() != null && !query.dateFilter().isBlank()) {
                date = LocalDate.parse(query.dateFilter().trim());
            }
            if (query != null && query.statusFilter() != null && !query.statusFilter().isBlank()) {
                status = DispatchStatus.valueOf(query.statusFilter().trim().toUpperCase(java.util.Locale.ROOT));
            }
        } catch (IllegalArgumentException exception) {
            throw new CommandValidationException("Invalid dispatch date or status filter.");
        }
        return assignmentRepository.search(date, status, pageable).map(this::resolveView);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public Page<DispatchAssignmentView> searchReportCandidates(String filter, Pageable pageable) {
        return assignmentRepository.searchReportCandidates(filter == null ? "" : filter.trim(), pageable)
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

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void hardDeleteAssignment(Long id) {
        DispatchAssignment assignment = assignmentRepository.findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch assignment not found: " + id));
        if (!assignment.isArchived() || (assignment.getDispatchStatus() != DispatchStatus.COMPLETED
                && assignment.getDispatchStatus() != DispatchStatus.CANCELLED)) {
            throw new BusinessConflictException("Archive a completed or cancelled assignment before permanently deleting it.");
        }
        List<String> blockers = assignmentRepository.findPermanentDeletionBlockers(id);
        if (!blockers.isEmpty()) {
            throw new BusinessConflictException("Cannot permanently delete this assignment because it has "
                    + String.join(", ", blockers) + ". Keep it archived to preserve those records.");
        }
        DispatchAssignmentView snapshot = resolveView(assignment);
        assignmentRepository.delete(assignment);
        assignmentRepository.flush();
        auditService.recordAction("DISPATCH_DELETED", "DispatchAssignment", id,
                "Permanently deleted archived assignment. Snapshot: " + snapshot + "; notes: " + assignment.getNotes()
                        + ". Business events retained under assignment ID " + id + ".");
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

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public DispatchAssignmentView getAssignment(Long id) {
        return resolveView(findAssignment(id));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public List<DispatchEventView> getHistory(Long assignmentId) {
        findAssignment(assignmentId);
        return eventRepository.findByAssignmentIdOrderByRecordedAtAscIdAsc(assignmentId).stream()
                .map(event -> new DispatchEventView(event.getAssignmentId(), event.getPreviousStatus(),
                        event.getNewStatus(), event.getEffectiveAt(), event.getRecordedAt(),
                        event.getActor(), event.getSourceType())).toList();
    }

    private void requireNotArchived(DispatchAssignment assignment) {
        if (assignment.isArchived()) {
            throw new CommandValidationException("Cannot change an archived assignment.");
        }
    }

    private void recordEvent(DispatchAssignment assignment, DispatchStatus previousStatus) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication == null ? "UNKNOWN" : authentication.getName();
        eventRepository.save(new DispatchEvent(assignment.getId(), previousStatus,
                assignment.getDispatchStatus(), actor, assignment.getUpdatedAt()));
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
    private DispatchAssignmentView resolveView(DispatchAssignment assignment) {
        RouteVersionView version = routeService.getVersion(assignment.getRouteVersionId());
        return new DispatchAssignmentView(assignment.getId(), assignment.getDispatchDate().toString(),
                assignment.getFleetUnitId(), resolveFleetUnitNumber(assignment.getFleetUnitId()),
                assignment.getDriverId(), resolveDriverName(assignment.getDriverId()),
                assignment.getRouteId(), version.routeCode(), assignment.getScheduledDeparture().toString(),
                assignment.getDispatchStatus().name(), assignment.getRouteVersionId(),
                formatInstant(assignment.getScheduledArrival()),
                formatInstant(assignment.getActualDeparture()), formatInstant(assignment.getActualArrival()),
                version.effectiveFrom().toString(),
                version.effectiveTo() == null ? null : version.effectiveTo().toString(),
                version.legacySnapshot(), assignment.getScheduleId());
    }

    private static String formatInstant(Instant value) {
        return value == null ? null : value.toString();
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
