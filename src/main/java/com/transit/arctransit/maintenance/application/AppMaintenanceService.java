package com.transit.arctransit.maintenance.application;

// maintenance module
import com.transit.arctransit.maintenance.CreateMaintenanceCommand;
import com.transit.arctransit.maintenance.MaintenanceRecordView;
import com.transit.arctransit.maintenance.MaintenanceService;
import com.transit.arctransit.maintenance.MaintenanceHistoryView;
import com.transit.arctransit.maintenance.domain.MaintenanceHistory;
import com.transit.arctransit.maintenance.domain.MaintenanceHistoryRepository;
import com.transit.arctransit.fleet.ChangeVehicleAvailabilityCommand;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.Instant;

// maintenance domain
import com.transit.arctransit.maintenance.domain.MaintenanceRecord;
import com.transit.arctransit.maintenance.domain.MaintenanceRepository;
import com.transit.arctransit.maintenance.domain.MaintenanceStatus;
import com.transit.arctransit.maintenance.domain.MaintenanceType;

// fleet
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitView;

// audit
import com.transit.arctransit.audit.AuditRecordingService;

// common
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;

// spring
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// java
import java.util.List;
import java.util.Locale;

// maintenance application service

@Service
@Validated
@Transactional
public class AppMaintenanceService
        implements MaintenanceService {

    // dependencies
    private final MaintenanceRepository repository;
    private final MaintenanceHistoryRepository historyRepository;

    private final FleetManagementService fleetService;

    private final AuditRecordingService auditService;

    // constructor
    public AppMaintenanceService(
            MaintenanceRepository repository,
            FleetManagementService fleetService,
            AuditRecordingService auditService,
            MaintenanceHistoryRepository historyRepository) {

        this.repository = repository;
        this.fleetService = fleetService;
        this.auditService = auditService;
        this.historyRepository = historyRepository;
    }

    // request maintenance
    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or "
                    + "hasRole('OPERATIONS_STAFF')"
    )
    public MaintenanceRecordView requestMaintenance(
            CreateMaintenanceCommand command) {

        FleetUnitView fleetUnit =
                fleetService.lockUnitForAssignment(command.fleetUnitId());
        if (fleetUnit.archivedAt() != null) {
            throw new CommandValidationException("Cannot request maintenance for an archived fleet unit.");
        }

        MaintenanceType type =
                parseMaintenanceType(
                        command.maintenanceType()
                );

        MaintenanceRecord record =
                new MaintenanceRecord(
                        command.fleetUnitId(),
                        type,
                        normalizeRequired(
                                command.description(),
                                "Description"
                        ),
                        command.scheduledDate()
                );

        MaintenanceRecord saved =
                repository.save(record);
        recordHistory(saved, null, "Maintenance requested.");

        auditService.recordAction(
                "MAINTENANCE_REQUESTED",
                "MaintenanceRecord",
                saved.getId(),
                "Maintenance requested for fleet unit "
                        + fleetUnit.unitNumber()
        );

        return toView(saved);
    }

    // approve maintenance
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public MaintenanceRecordView approveMaintenance(
            Long recordId) {

        MaintenanceRecord record =
                findRecord(recordId);

        fleetService.lockUnitForAssignment(record.getFleetUnitId());
        record.approve();
        recordHistory(record, MaintenanceStatus.REQUESTED, "Maintenance approved.");

        auditService.recordAction(
                "MAINTENANCE_APPROVED",
                "MaintenanceRecord",
                record.getId(),
                "Maintenance request approved"
        );

        return toView(record);
    }

    // start maintenance
    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or "
                    + "hasRole('OPERATIONS_STAFF')"
    )
    public MaintenanceRecordView startMaintenance(
            Long recordId) {

        MaintenanceRecord record =
                findRecord(recordId);

        FleetUnitView fleetUnit = fleetService.lockUnitForAssignment(record.getFleetUnitId());
        record.startWork();
        if ("AVAILABLE".equals(fleetUnit.availability()) || "UNKNOWN".equals(fleetUnit.availability())) {
            fleetService.changeAvailability(new ChangeVehicleAvailabilityCommand(
                    record.getFleetUnitId(), "MAINTENANCE", Instant.now(),
                    "Maintenance record " + record.getId() + " started."));
        }
        recordHistory(record, MaintenanceStatus.APPROVED, "Maintenance work started.");

        auditService.recordAction(
                "MAINTENANCE_STARTED",
                "MaintenanceRecord",
                record.getId(),
                "Maintenance work started; existing vehicle holds preserved."
        );

        return toView(record);
    }

    // complete maintenance
    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or "
                    + "hasRole('OPERATIONS_STAFF')"
    )
    public MaintenanceRecordView completeMaintenance(
            Long recordId,
            String technicianNotes) {

        MaintenanceRecord record =
                findRecord(recordId);

        FleetUnitView fleetUnit = fleetService.lockUnitForAssignment(record.getFleetUnitId());
        String normalizedNotes = normalizeRequired(technicianNotes, "Technician notes");

        record.complete(
                normalizedNotes
        );

        releaseMaintenanceAvailability(record, fleetUnit);
        recordHistory(record, MaintenanceStatus.IN_PROGRESS, "Maintenance work completed.");

        auditService.recordAction(
                "MAINTENANCE_COMPLETED",
                "MaintenanceRecord",
                record.getId(),
                "Maintenance completed; availability requires an explicit staff report."
        );
        auditService.recordAction("MAINTENANCE_ARCHIVED", "MaintenanceRecord", record.getId(),
                "Automatically archived after completion; record and history retained.");

        return toView(record);
    }

    // cancel maintenance
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public MaintenanceRecordView cancelMaintenance(
            Long recordId) {

        MaintenanceRecord record =
                findRecord(recordId);

        FleetUnitView fleetUnit = fleetService.lockUnitForAssignment(record.getFleetUnitId());
        MaintenanceStatus previous = record.getMaintenanceStatus();
        record.cancel();
        releaseMaintenanceAvailability(record, fleetUnit);
        recordHistory(record, previous, "Maintenance cancelled.");

        auditService.recordAction(
                "MAINTENANCE_CANCELLED",
                "MaintenanceRecord",
                record.getId(),
                "Maintenance cancelled"
        );

        return toView(record);
    }

    // count active maintenance
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public long countActiveMaintenanceForUnit(
            Long fleetUnitId) {

        return repository
                .countByFleetUnitIdAndMaintenanceStatusNotIn(
                        fleetUnitId,
                        List.of(
                                MaintenanceStatus.COMPLETED,
                                MaintenanceStatus.CANCELLED
                        )
                );
    }

    // search maintenance records
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public Page<MaintenanceRecordView> searchRecords(
            Pageable pageable) {

        return repository
                .findByArchivedAtIsNull(pageable)
                .map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Page<MaintenanceRecordView> searchArchivedRecords(String filter, Pageable pageable) {
        return repository.searchArchived(filter == null ? "" : filter.trim(), pageable).map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public MaintenanceRecordView getRecord(Long id) {
        return toView(findRecord(id));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public Page<MaintenanceHistoryView> history(Long id, Pageable pageable) {
        return historyRepository.findByMaintenanceRecordIdOrderByRecordedAtDescIdDesc(id, pageable)
                .map(event -> new MaintenanceHistoryView(event.getId(), event.getPreviousStatus(),
                        event.getStatus(), event.getRecordedAt(), event.getRecordedBy(), event.getReason()));
    }

    private void releaseMaintenanceAvailability(MaintenanceRecord record, FleetUnitView fleetUnit) {
        if (fleetUnit.archivedAt() == null && "MAINTENANCE".equals(fleetUnit.availability())
                && countActiveMaintenanceForUnit(record.getFleetUnitId()) == 0) {
            fleetService.changeAvailability(new ChangeVehicleAvailabilityCommand(
                    record.getFleetUnitId(), "UNKNOWN", Instant.now(),
                    "Maintenance record " + record.getId()
                            + " closed; current availability must be reported by staff."));
        }
    }

    private void recordHistory(MaintenanceRecord record, MaintenanceStatus previous, String reason) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication == null ? "SYSTEM" : authentication.getName();
        historyRepository.save(new MaintenanceHistory(record.getId(),
                previous == null ? null : previous.name(), record.getMaintenanceStatus().name(),
                Instant.now(), actor, reason));
    }

    // find record
    private MaintenanceRecord findRecord(
            Long id) {

        return repository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Maintenance record not found: "
                                        + id
                        )
                );
    }

    // parse maintenance type
    private static MaintenanceType
    parseMaintenanceType(
            String value) {

        if (value == null || value.isBlank()) {

            throw new CommandValidationException(
                    "Maintenance type is required."
            );
        }

        try {

            return MaintenanceType.valueOf(
                    value
                            .trim()
                            .toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException exception) {

            throw new CommandValidationException(
                    "Invalid maintenance type: "
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
    private MaintenanceRecordView toView(
            MaintenanceRecord record) {

        return new MaintenanceRecordView(
                record.getId(),
                record.getFleetUnitId(),
                record.getMaintenanceType().name(),
                record.getMaintenanceStatus().name(),
                record.getDescription(),
                record.getScheduledDate(),
                record.getStartedAt(),
                record.getCompletedAt(),
                record.getTechnicianNotes(),
                record.getCreatedAt()
        );
    }
}
