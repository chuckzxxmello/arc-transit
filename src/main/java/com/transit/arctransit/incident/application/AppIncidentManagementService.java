package com.transit.arctransit.incident.application;

// incident module
import com.transit.arctransit.incident.CreateIncidentCommand;
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
@Transactional
public class AppIncidentManagementService
        implements IncidentManagementService {

    // dependencies
    private final IncidentRepository repository;

    private final AuditRecordingService auditService;

    public AppIncidentManagementService(
            IncidentRepository repository,
            AuditRecordingService auditService) {

        this.repository = repository;
        this.auditService = auditService;
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

        Incident saved =
                repository.save(incident);

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
                normalizeOptional(
                        resolutionNotes
                )
        );

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

        auditService.recordAction(
                "INCIDENT_CLOSED",
                "Incident",
                incident.getId(),
                "Closed incident"
        );

        return toView(incident);
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
                incident.getResolutionNotes()
        );
    }
}