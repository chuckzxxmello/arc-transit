package com.transit.arctransit.reporting.application;

import com.transit.arctransit.audit.AuditRecordingService;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;
import com.transit.arctransit.dispatch.DispatchService;
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.reporting.*;
import com.transit.arctransit.reporting.domain.*;
import com.transit.arctransit.route.RouteManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@Validated
@Transactional
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
public class AppOperationalReportService implements OperationalReportService {

    private final OperationalReportRepository repository;
    private final ReportVerificationEventRepository eventRepository;
    private final AuditRecordingService auditService;
    private final FleetManagementService fleetService;
    private final DriverManagementService driverService;
    private final RouteManagementService routeService;
    private final DispatchService dispatchService;
    private final ReportingProperties properties;

    public AppOperationalReportService(OperationalReportRepository repository,
            ReportVerificationEventRepository eventRepository, AuditRecordingService auditService,
            FleetManagementService fleetService, DriverManagementService driverService,
            RouteManagementService routeService, DispatchService dispatchService,
            ReportingProperties properties) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.auditService = auditService;
        this.fleetService = fleetService;
        this.driverService = driverService;
        this.routeService = routeService;
        this.dispatchService = dispatchService;
        this.properties = properties;
    }

    @Override
    public OperationalReportView submitReport(CreateOperationalReportCommand command) {
        if (command.effectiveAt().isAfter(Instant.now())) {
            throw new CommandValidationException("Effective time cannot be in the future.");
        }
        ReportSourceType source;
        try {
            source = ReportSourceType.valueOf(command.sourceType().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new CommandValidationException("Source must be STAFF_ENTRY, PHONE or RADIO.");
        }
        String entityType = command.entityType().trim().toUpperCase(Locale.ROOT);
        switch (entityType) {
            case "FLEET" -> fleetService.lockUnitForAssignment(command.entityId());
            case "DRIVER" -> driverService.getDriver(command.entityId());
            case "ROUTE" -> routeService.getRoute(command.entityId());
            case "DISPATCH" -> dispatchService.getAssignment(command.entityId());
            default -> throw new CommandValidationException("Affected record must be FLEET, DRIVER, ROUTE or DISPATCH.");
        }
        String actor = currentUsername();
        OperationalReport report = repository.save(new OperationalReport(source, actor,
                command.effectiveAt(), entityType, command.entityId(), command.description().trim()));
        eventRepository.save(new ReportVerificationEvent(report.getId(), null, "UNVERIFIED", actor,
                "Report recorded for review"));
        auditService.recordAction("OPERATIONAL_REPORT_SUBMITTED", "OperationalReport", report.getId(),
                "Recorded source " + source + " for " + entityType + " " + command.entityId());
        return toView(report);
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public OperationalReportView verifyReport(Long id, String status, String reason) {
        VerificationStatus target;
        try {
            target = VerificationStatus.valueOf(status == null ? "" : status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new CommandValidationException("Unknown verification status.");
        }
        OperationalReport report = findLockedReport(id);
        String previous = report.getVerificationStatus().name();
        report.verify(target, currentUsername(), reason);
        eventRepository.save(new ReportVerificationEvent(id, previous, target.name(),
                currentUsername(), report.getVerificationReason()));
        auditService.recordAction("OPERATIONAL_REPORT_VERIFIED", "OperationalReport", id,
                previous + " -> " + target);
        return toView(report);
    }

    @Override
    @Transactional(readOnly = true)
    public OperationalReportView getReport(Long id) {
        return toView(findReport(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OperationalReportView> searchReports(Pageable pageable) {
        return repository.findByArchivedAtIsNull(pageable).map(this::toView);
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void archiveReport(Long id) {
        OperationalReport report = findLockedReport(id);
        if (report.getArchivedAt() != null) return;
        report.archive();
        auditService.recordAction("OPERATIONAL_REPORT_ARCHIVED", "OperationalReport", id,
                "Archived report; verification remains " + report.getVerificationStatus());
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void restoreReport(Long id) {
        OperationalReport report = findLockedReport(id);
        if (report.getArchivedAt() == null) return;
        report.restore();
        auditService.recordAction("OPERATIONAL_REPORT_RESTORED", "OperationalReport", id,
                "Restored report; verification remains " + report.getVerificationStatus());
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void hardDeleteReport(Long id) {
        OperationalReport report = findLockedReport(id);
        if (report.getArchivedAt() == null) {
            throw new BusinessConflictException("Archive this operational report before permanently deleting it.");
        }
        List<String> blockers = repository.findPermanentDeletionBlockers(id);
        if (!blockers.isEmpty()) {
            throw new BusinessConflictException("Cannot permanently delete this report because it has linked "
                    + String.join(", ", blockers) + ". Keep it archived to preserve those records.");
        }
        OperationalReportView snapshot = toView(report);
        repository.delete(report);
        repository.flush();
        auditService.recordAction("OPERATIONAL_REPORT_DELETED", "OperationalReport", id,
                "Permanently deleted archived report. Snapshot: " + snapshot
                        + ". Verification history retained under report ID " + id + ".");
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Page<OperationalReportView> searchArchivedReports(String filter, Pageable pageable) {
        return repository.searchArchived(filter == null ? "" : filter.trim(), pageable).map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperationalReportView> getReviewCandidates(Instant staleBefore) {
        return repository.findReviewCandidates(staleBefore).stream().map(this::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportVerificationView> getVerificationHistory(Long id) {
        findReport(id);
        return eventRepository.findByReportIdOrderByRecordedAtAscIdAsc(id).stream()
                .map(event -> new ReportVerificationView(event.getPreviousStatus(), event.getNewStatus(),
                        event.getRecordedBy(), event.getRecordedAt(), event.getReason())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnverifiedReports() {
        return repository.countByArchivedAtIsNullAndVerificationStatus(VerificationStatus.UNVERIFIED);
    }

    @Override
    @Transactional(readOnly = true)
    public long countStaleReports() {
        return repository.countByArchivedAtIsNullAndEffectiveAtLessThanEqualAndVerificationStatusNot(
                Instant.now().minus(properties.staleAfter()), VerificationStatus.REJECTED);
    }

    private OperationalReport findReport(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Operational report not found: " + id));
    }

    private OperationalReport findLockedReport(Long id) {
        return repository.findLockedById(id).orElseThrow(() ->
                new ResourceNotFoundException("Operational report not found: " + id));
    }

    private static String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private OperationalReportView toView(OperationalReport report) {
        return new OperationalReportView(report.getId(), report.getSourceType().name(), report.getReportedBy(),
                report.getReportedAt(), report.getEffectiveAt(), report.getVerificationStatus().name(),
                report.getVerifiedBy(), report.getVerifiedAt(), report.getEntityType(), report.getEntityId(),
                report.getDescription(), properties.freshness(report.getEffectiveAt(), Instant.now()),
                report.getVerificationReason(), report.getArchivedAt());
    }
}
