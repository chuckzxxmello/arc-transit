package com.transit.arctransit.incident.application;

// incident module
import com.transit.arctransit.incident.CreateIncidentCommand;
import com.transit.arctransit.incident.IncidentEventView;
import com.transit.arctransit.incident.domain.IncidentEvent;
import com.transit.arctransit.incident.domain.IncidentEventRepository;
import com.transit.arctransit.reporting.OperationalReportService;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.route.RouteManagementService;
import com.transit.arctransit.dispatch.DispatchService;
import com.transit.arctransit.maintenance.MaintenanceService;
import org.springframework.validation.annotation.Validated;
import java.time.Instant;
import com.transit.arctransit.incident.IncidentManagementService;
import com.transit.arctransit.incident.IncidentView;

// incident domain
import com.transit.arctransit.incident.domain.AffectedServiceType;
import com.transit.arctransit.incident.domain.Incident;
import com.transit.arctransit.incident.domain.IncidentRepository;
import com.transit.arctransit.incident.domain.IncidentSeverity;
import com.transit.arctransit.incident.domain.IncidentStatus;

// audit
import com.transit.arctransit.audit.AuditRecordingService;

// common
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;

// security
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// spring
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// java
import java.util.List;
import java.util.Locale;

// incident application service
@Service
@Validated
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
@Transactional
public class AppIncidentManagementService
        implements IncidentManagementService {

    // dependencies
    private final IncidentRepository repository;

    private final AuditRecordingService auditService;

    private final IncidentEventRepository eventRepository;
    private final OperationalReportService reportService;
    private final FleetManagementService fleetService;
    private final DriverManagementService driverService;
    private final RouteManagementService routeService;
    private final DispatchService dispatchService;
    private final MaintenanceService maintenanceService;

    public AppIncidentManagementService(
            IncidentRepository repository,
            AuditRecordingService auditService,
            IncidentEventRepository eventRepository,
            OperationalReportService reportService,
            FleetManagementService fleetService,
            DriverManagementService driverService,
            RouteManagementService routeService,
            DispatchService dispatchService,
            MaintenanceService maintenanceService) {

        this.repository = repository;
        this.auditService = auditService;
        this.eventRepository = eventRepository;
        this.reportService = reportService;
        this.fleetService = fleetService;
        this.driverService = driverService;
        this.routeService = routeService;
        this.dispatchService = dispatchService;
        this.maintenanceService = maintenanceService;
    }

    // report incident
    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or "
                    + "hasRole('OPERATIONS_STAFF')"
    )
    public IncidentView reportIncident(
            CreateIncidentCommand command) {

        String reporter =
                getCurrentUsername();

        IncidentSeverity severity =
                parseSeverity(
                        command.severity()
                );

        AffectedServiceType serviceType =
                parseOptionalAffectedService(
                        command.affectedServiceType()
                );

        Instant effectiveAt = command.effectiveAt();
        if (effectiveAt == null || effectiveAt.isAfter(Instant.now())) {
            throw new CommandValidationException("Effective time is required and cannot be in the future.");
        }
        validateAffectedRecord(serviceType, command.affectedEntityId());
        if (command.operationalReportId() != null) {
            var report = reportService.getReport(command.operationalReportId());
            if (serviceType == null || command.affectedEntityId() == null
                    || !serviceType.name().equals(report.entityType())
                    || !command.affectedEntityId().equals(report.entityId())) {
                throw new CommandValidationException("The report must reference the same affected record as the incident.");
            }
        }

        Incident incident =
                new Incident(
                        normalizeRequired(
                                command.title(),
                                "Title"
                        ),
                        normalizeOptional(
                                command.description()
                        ),
                        severity,
                        serviceType,
                        command.affectedEntityId(),
                        reporter
                );

        incident.setReportContext(effectiveAt, command.operationalReportId());

        Incident saved =
                repository.save(incident);

        appendEvent(saved, null, "Incident reported");

        auditService.recordAction(
                "INCIDENT_REPORTED",
                "Incident",
                saved.getId(),
                "Reported: "
                        + saved.getTitle()
        );

        return toView(saved);
    }

    // acknowledge
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public IncidentView acknowledgeIncident(
            Long incidentId) {

        Incident incident =
                findIncident(incidentId);

        incident.acknowledge();
        appendEvent(incident, "REPORTED", "acknowledge");

        auditService.recordAction(
                "INCIDENT_ACKNOWLEDGED",
                "Incident",
                incident.getId(),
                "Acknowledged incident"
        );

        return toView(incident);
    }

    // start investigation
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public IncidentView startInvestigation(
            Long incidentId) {

        Incident incident =
                findIncident(incidentId);

        incident.startInvestigation();
        appendEvent(incident, "ACKNOWLEDGED", "startInvestigation");

        auditService.recordAction(
                "INCIDENT_INVESTIGATING",
                "Incident",
                incident.getId(),
                "Started investigation"
        );

        return toView(incident);
    }

    // resolve
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public IncidentView resolveIncident(
            Long incidentId,
            String resolutionNotes) {

        Incident incident =
                findIncident(incidentId);

        incident.resolve(
                normalizeRequired(
                        resolutionNotes, "Resolution notes"
                )
        );

        appendEvent(incident, "INVESTIGATING", incident.getResolutionNotes());

        auditService.recordAction(
                "INCIDENT_RESOLVED",
                "Incident",
                incident.getId(),
                "Resolved incident"
        );

        return toView(incident);
    }

    // close
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public IncidentView closeIncident(
            Long incidentId) {

        Incident incident =
                findIncident(incidentId);

        incident.close();
        appendEvent(incident, "RESOLVED", "close");

        auditService.recordAction(
                "INCIDENT_CLOSED",
                "Incident",
                incident.getId(),
                "Closed incident"
        );
        auditService.recordAction("INCIDENT_ARCHIVED", "Incident", incident.getId(),
                "Automatically archived after closure; record and history retained.");

        return toView(incident);
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public IncidentView linkMaintenanceRecord(Long incidentId, Long maintenanceRecordId) {
        Incident incident = findIncident(incidentId);
        var maintenance = maintenanceService.getRecord(maintenanceRecordId);
        Long fleetUnitId = null;
        if (incident.getAffectedServiceType() == AffectedServiceType.FLEET) {
            fleetUnitId = incident.getAffectedEntityId();
        } else if (incident.getAffectedServiceType() == AffectedServiceType.DISPATCH
                && incident.getAffectedEntityId() != null) {
            fleetUnitId = dispatchService.getAssignment(incident.getAffectedEntityId()).fleetUnitId();
        }
        if (fleetUnitId == null || !fleetUnitId.equals(maintenance.fleetUnitId())) {
            throw new CommandValidationException("Maintenance must concern the incident's affected vehicle or trip vehicle.");
        }
        incident.linkMaintenanceRecord(maintenanceRecordId);
        appendEvent(incident, incident.getStatus().name(), "Linked maintenance record " + maintenanceRecordId);
        auditService.recordAction("INCIDENT_MAINTENANCE_LINKED", "Incident", incidentId,
                "Maintenance record " + maintenanceRecordId);
        return toView(incident);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentEventView> getHistory(Long incidentId) {
        findIncident(incidentId);
        return eventRepository.findByIncidentIdOrderByRecordedAtAscIdAsc(incidentId).stream()
                .map(event -> new IncidentEventView(event.getPreviousStatus(), event.getNewStatus(),
                        event.getEffectiveAt(), event.getRecordedAt(), event.getRecordedBy(), event.getReason())).toList();
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public IncidentView unlinkMaintenanceRecord(Long incidentId, Long expectedMaintenanceRecordId, String reason) {
        Incident incident = findIncident(incidentId);
        String notes = normalizeRequired(reason, "Reason for unlinking");
        incident.unlinkMaintenanceRecord(expectedMaintenanceRecordId);
        appendEvent(incident, incident.getStatus().name(),
                "Unlinked maintenance record " + expectedMaintenanceRecordId + ": " + notes);
        auditService.recordAction("INCIDENT_MAINTENANCE_UNLINKED", "Incident", incidentId,
                "Unlinked maintenance record " + expectedMaintenanceRecordId + ": " + notes);
        return toView(incident);
    }

    private void validateAffectedRecord(AffectedServiceType type, Long id) {
        if (id == null) {
            return;
        }
        if (type == null || id <= 0) {
            throw new CommandValidationException("Affected record type and a positive ID are required together.");
        }
        switch (type) {
            case FLEET -> fleetService.lockUnitForAssignment(id);
            case DRIVER -> driverService.getDriver(id);
            case ROUTE -> routeService.getRoute(id);
            case DISPATCH -> dispatchService.getAssignment(id);
        }
    }

    private void appendEvent(Incident incident, String previous, String reason) {
        eventRepository.save(new IncidentEvent(incident.getId(), previous, incident.getStatus().name(),
                previous == null ? incident.getEffectiveAt() : Instant.now(), getCurrentUsername(), reason));
    }

    // recent incidents
    @Override
    @Transactional(readOnly = true)
    public List<IncidentView> getRecentIncidents() {

        return repository
                .findTop10ByOrderByReportedAtDesc()
                .stream()
                .map(this::toView)
                .toList();
    }

    // open incident count
    @Override
    @Transactional(readOnly = true)
    public long countOpenIncidents() {

        return repository.countByStatusIn(
                List.of(
                        IncidentStatus.REPORTED,
                        IncidentStatus.ACKNOWLEDGED,
                        IncidentStatus.INVESTIGATING
                )
        );
    }

    // search active incidents
    @Override
    @Transactional(readOnly = true)
    public Page<IncidentView> searchIncidents(
            Pageable pageable) {

        return repository
                .findByArchivedAtIsNull(pageable)
                .map(this::toView);
    }

    // find incident
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Page<IncidentView> searchArchivedIncidents(String filter, Pageable pageable) {
        return repository.searchArchived(filter == null ? "" : filter.trim(), pageable).map(this::toView);
    }

    private Incident findIncident(
            Long id) {

        return repository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Incident not found: "
                                        + id
                        )
                );
    }

    // resolve current username
    private static String getCurrentUsername() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication
                        instanceof AnonymousAuthenticationToken) {

            return "SYSTEM";
        }

        String username =
                authentication.getName();

        return username == null
                || username.isBlank()
                ? "SYSTEM"
                : username;
    }

    // parse incident severity
    private static IncidentSeverity parseSeverity(
            String value) {

        if (value == null || value.isBlank()) {

            throw new CommandValidationException(
                    "Severity is required."
            );
        }

        try {

            return IncidentSeverity.valueOf(
                    value
                            .trim()
                            .toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException exception) {

            throw new CommandValidationException(
                    "Invalid incident severity: "
                            + value
            );
        }
    }

    // parse affected service
    private static AffectedServiceType
    parseOptionalAffectedService(
            String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {

            return AffectedServiceType.valueOf(
                    value
                            .trim()
                            .toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException exception) {

            throw new CommandValidationException(
                    "Invalid affected service type: "
                            + value
            );
        }
    }

    // normalize required text
    private static String normalizeRequired(
            String value,
            String fieldName) {

        if (value == null || value.isBlank()) {

            throw new CommandValidationException(
                    fieldName
                            + " cannot be blank."
            );
        }

        return value.trim();
    }

    // normalize optional text
    private static String normalizeOptional(
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

    // map entity to view
    private IncidentView toView(
            Incident incident) {

        return new IncidentView(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getSeverity().name(),
                incident.getStatus().name(),
                incident.getAffectedServiceType() != null
                        ? incident
                                .getAffectedServiceType()
                                .name()
                        : null,
                incident.getAffectedEntityId(),
                incident.getReportedBy(),
                incident.getReportedAt(),
                incident.getAcknowledgedAt(),
                incident.getResolvedAt(),
                incident.getResolutionNotes(),
                incident.getEffectiveAt(),
                incident.getSourceType(),
                incident.getOperationalReportId(),
                incident.getMaintenanceRecordId()
        );
    }
}
