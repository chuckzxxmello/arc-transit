package com.transit.arctransit.reconciliation.application;

// audit

import com.transit.arctransit.audit.AuditRecordingService;

// common

import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;

// dispatch

import com.transit.arctransit.dispatch.DispatchAssignmentView;
import com.transit.arctransit.dispatch.DispatchService;

// fleet

import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitView;
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.driver.DriverView;
import com.transit.arctransit.maintenance.MaintenanceRecordView;
import com.transit.arctransit.reporting.OperationalReportService;
import com.transit.arctransit.reporting.ReportingProperties;

// incident

import com.transit.arctransit.incident.IncidentManagementService;
import com.transit.arctransit.incident.IncidentView;

// maintenance

import com.transit.arctransit.maintenance.MaintenanceService;

// reconciliation

import com.transit.arctransit.reconciliation.ExceptionSummaryView;
import com.transit.arctransit.reconciliation.OperationalExceptionView;
import com.transit.arctransit.reconciliation.ReconciliationProperties;
import com.transit.arctransit.reconciliation.ReconciliationService;
import com.transit.arctransit.reconciliation.domain.ExceptionStatus;
import com.transit.arctransit.reconciliation.domain.ExceptionType;
import com.transit.arctransit.reconciliation.domain.OperationalException;
import com.transit.arctransit.reconciliation.domain.OperationalExceptionRepository;

// spring

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// java

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// reconciliation application service

@Service
@org.springframework.validation.annotation.Validated
@Transactional
public class AppReconciliationService
        implements ReconciliationService {

    // constants

    private static final int PAGE_SIZE = 500;

    private static final List<ExceptionStatus> OPEN_STATUSES =
            List.of(
                    ExceptionStatus.OPEN,
                    ExceptionStatus.ACKNOWLEDGED,
                    ExceptionStatus.INVESTIGATING
            );

    private static final Set<String> OPEN_INCIDENT_STATUSES =
            Set.of(
                    "REPORTED",
                    "ACKNOWLEDGED",
                    "INVESTIGATING"
            );

    // dependencies

    private final OperationalExceptionRepository repository;

    private final ReconciliationProperties properties;

    private final AuditRecordingService auditService;

    private final FleetManagementService fleetService;

    private final MaintenanceService maintenanceService;

    private final DispatchService dispatchService;

    private final IncidentManagementService incidentService;
    private final DriverManagementService driverService;
    private final OperationalReportService reportService;
    private final ReportingProperties reportingProperties;

    // constructor

    public AppReconciliationService(
            OperationalExceptionRepository repository,
            ReconciliationProperties properties,
            AuditRecordingService auditService,
            FleetManagementService fleetService,
            MaintenanceService maintenanceService,
            DispatchService dispatchService,
            IncidentManagementService incidentService,
            DriverManagementService driverService,
            OperationalReportService reportService,
            ReportingProperties reportingProperties) {

        this.repository = repository;
        this.properties = properties;
        this.auditService = auditService;
        this.fleetService = fleetService;
        this.maintenanceService = maintenanceService;
        this.dispatchService = dispatchService;
        this.incidentService = incidentService;
        this.driverService = driverService;
        this.reportService = reportService;
        this.reportingProperties = reportingProperties;
    }

    // run reconciliation

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void runReconciliation() {
        repository.lockForScan();

        auditService.recordAction(
                "RECONCILIATION_STARTED",
                "ReconciliationEngine",
                0L,
                "Reconciliation checks started."
        );

        try {

            checkContradictions();
            checkStaleStates();
            checkOperationalReports();

            auditService.recordAction(
                    "RECONCILIATION_COMPLETED",
                    "ReconciliationEngine",
                    0L,
                    "Reconciliation checks completed."
            );

        } catch (RuntimeException exception) {

            auditService.recordFailure(
                    "RECONCILIATION_FAILED",
                    "ReconciliationEngine",
                    0L,
                    "Reconciliation failed."
            );

            throw exception;
        }
    }

    // contradiction checks

    private void checkContradictions() {
        Map<Long, FleetUnitView> units = new HashMap<>();
        Map<Long, DriverView> drivers = new HashMap<>();
        Set<Long> maintenanceHolds = new HashSet<>();
        Set<Long> workInProgress = new HashSet<>();
        int pageNumber = 0;
        Page<MaintenanceRecordView> maintenancePage;
        do {
            maintenancePage = maintenanceService.searchRecords(PageRequest.of(pageNumber++, PAGE_SIZE));
            for (MaintenanceRecordView record : maintenancePage) {
                if (Set.of("REQUESTED", "APPROVED", "IN_PROGRESS").contains(record.maintenanceStatus())) {
                    maintenanceHolds.add(record.fleetUnitId());
                }
                if ("IN_PROGRESS".equals(record.maintenanceStatus())) {
                    workInProgress.add(record.fleetUnitId());
                }
            }
        } while (maintenancePage.hasNext());

        for (Long unitId : workInProgress) {
            FleetUnitView unit = units.computeIfAbsent(unitId, fleetService::getUnit);
            if ("AVAILABLE".equals(unit.availability())) {
                recordExceptionIfNotExists(ExceptionType.CONTRADICTION,
                        "MAINTENANCE_AVAILABILITY_CONFLICT", "HIGH", "FLEET", unitId,
                        "Fleet unit " + unit.unitNumber()
                                + " is recorded AVAILABLE while maintenance is IN_PROGRESS.");
            }
        }

        List<DispatchAssignmentView> activeAssignments = new ArrayList<>();
        pageNumber = 0;
        Page<DispatchAssignmentView> dispatchPage;
        do {
            dispatchPage = dispatchService.searchAssignments(null, PageRequest.of(pageNumber++, PAGE_SIZE));
            for (DispatchAssignmentView assignment : dispatchPage) {
                if (Set.of("SCHEDULED", "IN_PROGRESS").contains(assignment.dispatchStatus())) {
                    activeAssignments.add(assignment);
                }
            }
        } while (dispatchPage.hasNext());

        for (DispatchAssignmentView assignment : activeAssignments) {
            FleetUnitView unit = units.computeIfAbsent(assignment.fleetUnitId(), fleetService::getUnit);
            if (unit.archivedAt() != null || !"ACTIVE".equals(unit.administrativeStatus())
                    || !"AVAILABLE".equals(unit.availability())) {
                recordExceptionIfNotExists(ExceptionType.CONTRADICTION,
                        "ASSIGNMENT_VEHICLE_UNAVAILABLE", "HIGH", "DISPATCH", assignment.id(),
                        "Active dispatch " + assignment.id() + " references fleet unit " + unit.id()
                                + " whose recorded administrative state or availability prevents assignment.");
            }
            if (maintenanceHolds.contains(assignment.fleetUnitId())) {
                recordExceptionIfNotExists(ExceptionType.CONTRADICTION,
                        "ASSIGNMENT_MAINTENANCE_HOLD", "HIGH", "DISPATCH", assignment.id(),
                        "Active dispatch " + assignment.id() + " references fleet unit "
                                + unit.id() + " with an active maintenance hold.");
            }
            DriverView driver = drivers.computeIfAbsent(assignment.driverId(), driverService::getDriver);
            if (driver.archivedAt() != null || !"ACTIVE".equals(driver.employmentStatus())
                    || !"AVAILABLE".equals(driver.availability())) {
                recordExceptionIfNotExists(ExceptionType.CONTRADICTION,
                        "ASSIGNMENT_DRIVER_UNAVAILABLE", "HIGH", "DISPATCH", assignment.id(),
                        "Active dispatch " + assignment.id() + " references unavailable driver " + driver.id() + ".");
            }
            LocalDate serviceDate = LocalDate.parse(assignment.dispatchDate());
            if (driver.licenseExpiryDate() == null
                    || LocalDate.parse(driver.licenseExpiryDate()).isBefore(serviceDate)) {
                recordExceptionIfNotExists(ExceptionType.CONTRADICTION,
                        "ASSIGNMENT_DRIVER_LICENSE_EXPIRED", "HIGH", "DISPATCH", assignment.id(),
                        "Driver " + driver.id() + " qualification is not valid for active dispatch " + assignment.id() + ".");
            }
            if (assignment.routeVersionId() == null
                    || (!assignment.legacyRouteSnapshot()
                        && (assignment.routeVersionEffectiveFrom() == null
                            || serviceDate.isBefore(LocalDate.parse(assignment.routeVersionEffectiveFrom()))
                            || (assignment.routeVersionEffectiveTo() != null
                                && !serviceDate.isBefore(LocalDate.parse(assignment.routeVersionEffectiveTo())))))) {
                recordExceptionIfNotExists(ExceptionType.CONTRADICTION,
                        "ASSIGNMENT_ROUTE_VERSION_INVALID", "HIGH", "DISPATCH", assignment.id(),
                        "Active dispatch " + assignment.id() + " has no route version applicable to its service date.");
            }
        }
    }

    private void checkOperationalReports() {
        Instant staleBefore = Instant.now().minus(reportingProperties.staleAfter());
        for (var report : reportService.getReviewCandidates(staleBefore)) {
            if ("UNVERIFIED".equals(report.verificationStatus())) {
                recordExceptionIfNotExists(ExceptionType.MISSING_DATA,
                        "UNVERIFIED_OPERATIONAL_REPORT", "MEDIUM", "OPERATIONAL_REPORT", report.id(),
                        "Operational report " + report.id() + " remains unverified and requires human review.");
            }
            if (!Set.of("REJECTED", "SUPERSEDED").contains(report.verificationStatus())
                    && !report.effectiveAt().isAfter(staleBefore)) {
                recordExceptionIfNotExists(ExceptionType.STALE_STATE,
                        "STALE_OPERATIONAL_REPORT", "MEDIUM", "OPERATIONAL_REPORT", report.id(),
                        "Operational report " + report.id() + " is older than the configured freshness threshold.");
            }
        }
    }

    // stale state checks

    private void checkStaleStates() {

        checkStaleDispatches();
        checkStaleIncidents();
    }

    // stale dispatch detection

    private void checkStaleDispatches() {

        Instant threshold =
                Instant.now()
                        .minus(
                                properties
                                        .dispatchStalenessThreshold()
                        );

        int pageNumber = 0;

        while (true) {

            Page<DispatchAssignmentView> page =
                    dispatchService.searchAssignments(
                            null,
                            PageRequest.of(
                                    pageNumber,
                                    PAGE_SIZE
                            )
                    );

            for (DispatchAssignmentView dispatch : page) {

                if (!"IN_PROGRESS".equals(
                        dispatch.dispatchStatus()
                )) {

                    continue;
                }

                Instant scheduledDeparture = Instant.parse(
                        dispatch.actualDeparture() != null
                                ? dispatch.actualDeparture() : dispatch.scheduledDeparture());

                if (scheduledDeparture.isBefore(
                        threshold
                )) {

                    recordExceptionIfNotExists(
                            ExceptionType.STALE_STATE,
                            "AGING_ACTIVE_TRIP",
                            "MEDIUM",
                            "DISPATCH",
                            dispatch.id(),
                            "Dispatch "
                                    + dispatch.id()
                                    + " for unit "
                                    + dispatch.fleetUnitNumber()
                                    + " has remained IN_PROGRESS "
                                    + "beyond the configured threshold."
                    );
                }
            }

            if (!page.hasNext()) {
                break;
            }

            pageNumber++;
        }
    }

    // stale incident detection

    private void checkStaleIncidents() {

        Instant threshold =
                Instant.now()
                        .minus(
                                properties
                                        .incidentResolutionDeadline()
                        );

        int pageNumber = 0;

        while (true) {

            Page<IncidentView> page =
                    incidentService.searchIncidents(
                            PageRequest.of(
                                    pageNumber,
                                    PAGE_SIZE
                            )
                    );

            for (IncidentView incident : page) {

                if (!OPEN_INCIDENT_STATUSES.contains(
                        incident.status()
                )) {

                    continue;
                }

                if (incident.reportedAt() == null) {
                    continue;
                }

                if (incident.reportedAt().isBefore(
                        threshold
                )) {

                    recordExceptionIfNotExists(
                            ExceptionType.STALE_STATE,
                            "AGING_OPEN_INCIDENT",
                            "MEDIUM",
                            "INCIDENT",
                            incident.id(),
                            "Incident "
                                    + incident.id()
                                    + " remains unresolved beyond "
                                    + "the configured resolution deadline."
                    );
                }
            }

            if (!page.hasNext()) {
                break;
            }

            pageNumber++;
        }
    }

    // create exception only when no active duplicate exists

    private void recordExceptionIfNotExists(
            ExceptionType type,
            String ruleCode,
            String severity,
            String entityType,
            Long entityId,
            String description) {

        if ("FLEET".equals(entityType)) {
            try {
                fleetService.lockUnitForAssignment(entityId);
            } catch (ResourceNotFoundException exception) {
                // A bus can be deleted after the reconciliation scan reads its snapshot.
                return;
            }
        }

        boolean exists =
                repository
                        .existsByRuleCodeAndAffectedEntityTypeAndAffectedEntityIdAndExceptionStatusIn(
                                ruleCode,
                                entityType,
                                entityId,
                                OPEN_STATUSES
                        );

        if (exists) {
            return;
        }

        OperationalException exception =
                new OperationalException(
                        type,
                        ruleCode,
                        severity,
                        entityType,
                        entityId,
                        description
                );

        OperationalException saved =
                repository.save(exception);

        auditService.recordAction(
                "EXCEPTION_DETECTED",
                "OperationalException",
                saved.getId(),
                description
        );
    }

    // acknowledge

    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or "
                    + "hasRole('OPERATIONS_STAFF')"
    )
    public OperationalExceptionView acknowledgeException(
            Long exceptionId) {

        OperationalException exception =
                findException(exceptionId);

        String username =
                requireCurrentUsername();

        exception.acknowledge(
                username
        );

        auditService.recordAction(
                "EXCEPTION_ACKNOWLEDGED",
                "OperationalException",
                exception.getId(),
                "Acknowledged by " + username
        );

        return toView(exception);
    }

    // start investigation

    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or "
                    + "hasRole('OPERATIONS_STAFF')"
    )
    public OperationalExceptionView startInvestigation(
            Long exceptionId) {

        OperationalException exception =
                findException(exceptionId);

        exception.startInvestigation();

        auditService.recordAction(
                "EXCEPTION_INVESTIGATING",
                "OperationalException",
                exception.getId(),
                "Investigation started."
        );

        return toView(exception);
    }

    // resolve

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public OperationalExceptionView resolveException(
            Long exceptionId,
            String notes) {

        OperationalException exception =
                findException(exceptionId);

        String username =
                requireCurrentUsername();

        String normalizedNotes =
                normalizeNotes(notes);

        exception.resolve(
                username,
                normalizedNotes
        );

        auditService.recordAction(
                "EXCEPTION_RESOLVED",
                "OperationalException",
                exception.getId(),
                "Resolved by " + username
        );

        return toView(exception);
    }

    // dismiss

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public OperationalExceptionView dismissException(
            Long exceptionId,
            String notes) {

        OperationalException exception =
                findException(exceptionId);

        String username =
                requireCurrentUsername();

        String normalizedNotes =
                normalizeNotes(notes);

        exception.dismiss(
                username,
                normalizedNotes
        );

        auditService.recordAction(
                "EXCEPTION_DISMISSED",
                "OperationalException",
                exception.getId(),
                "Dismissed by " + username
        );

        return toView(exception);
    }

    // summary

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or "
                    + "hasRole('OPERATIONS_STAFF')"
    )
    public ExceptionSummaryView getExceptionSummary() {

        long total =
                repository.countByExceptionStatusIn(
                        OPEN_STATUSES
                );

        Map<String, Long> byType =
                java.util.Arrays
                        .stream(ExceptionType.values())
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        ExceptionType::name,
                                        type ->
                                                repository
                                                        .countByExceptionTypeAndExceptionStatusIn(
                                                                type,
                                                                OPEN_STATUSES
                                                        )
                                )
                        );

        Map<String, Long> bySeverity =
                List.of(
                        "LOW",
                        "MEDIUM",
                        "HIGH",
                        "CRITICAL"
                )
                .stream()
                .collect(
                        Collectors.toUnmodifiableMap(
                                severity -> severity,
                                severity ->
                                        repository
                                                .countBySeverityAndExceptionStatusIn(
                                                        severity,
                                                        OPEN_STATUSES
                                                )
                        )
                );

        return new ExceptionSummaryView(
                total,
                byType,
                bySeverity
        );
    }

    // search exceptions

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or "
                    + "hasRole('OPERATIONS_STAFF')"
    )
    public Page<OperationalExceptionView> searchExceptions(
            Pageable pageable) {

        return repository
                .findAllByOrderByDetectedAtDesc(pageable)
                .map(this::toView);
    }

    // find exception

    private OperationalException findException(
            Long id) {

        return repository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Operational exception not found: "
                                        + id
                        )
                );
    }

    // current username

    private static String requireCurrentUsername() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication
                        instanceof AnonymousAuthenticationToken) {

            throw new CommandValidationException(
                    "No authenticated user session."
            );
        }

        String username =
                authentication.getName();

        if (username == null || username.isBlank()) {

            throw new CommandValidationException(
                    "Authenticated username is unavailable."
            );
        }

        return username;
    }

    // normalize notes

    private static String normalizeNotes(
            String notes) {

        if (notes == null || notes.isBlank()) {
            throw new CommandValidationException("Resolution notes are required.");
        }
        return notes.trim();
    }

    // map entity to view

    private OperationalExceptionView toView(
            OperationalException exception) {

        return new OperationalExceptionView(
                exception.getId(),
                exception.getExceptionType().name(),
                exception.getSeverity(),
                exception.getAffectedEntityType(),
                exception.getAffectedEntityId(),
                exception.getDescription(),
                exception.getExceptionStatus().name(),
                exception.getDetectedAt(),
                exception.getAcknowledgedBy(),
                exception.getAcknowledgedAt(),
                exception.getResolvedBy(),
                exception.getResolvedAt(),
                exception.getResolutionNotes(),
                exception.getRuleCode()
        );
    }
}