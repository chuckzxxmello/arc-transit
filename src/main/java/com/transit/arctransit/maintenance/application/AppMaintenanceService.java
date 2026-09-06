package com.transit.arctransit.maintenance.application;

// maintenance module
import com.transit.arctransit.maintenance.CreateMaintenanceCommand;
import com.transit.arctransit.maintenance.MaintenanceRecordView;
import com.transit.arctransit.maintenance.MaintenanceService;

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
@Transactional
public class AppMaintenanceService
        implements MaintenanceService {

    // dependencies
    private final MaintenanceRepository repository;

    private final FleetManagementService fleetService;

    private final AuditRecordingService auditService;

    // constructor
    public AppMaintenanceService(
            MaintenanceRepository repository,
            FleetManagementService fleetService,
            AuditRecordingService auditService) {

        this.repository = repository;
        this.fleetService = fleetService;
        this.auditService = auditService;
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
                fleetService.getUnit(
                        command.fleetUnitId()
                );

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

        record.approve();

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

        record.startWork();

        // fleet status changes with maintenance lifecycle
        fleetService.changeStatus(
                record.getFleetUnitId(),
                "UNDER_MAINTENANCE"
        );

        auditService.recordAction(
                "MAINTENANCE_STARTED",
                "MaintenanceRecord",
                record.getId(),
                "Maintenance started. Fleet unit moved "
                        + "to UNDER_MAINTENANCE."
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

        String normalizedNotes =
                normalizeOptional(
                        technicianNotes
                );

        record.complete(
                normalizedNotes
        );

        // return fleet unit to active state
        fleetService.changeStatus(
                record.getFleetUnitId(),
                "ACTIVE"
        );

        auditService.recordAction(
                "MAINTENANCE_COMPLETED",
                "MaintenanceRecord",
                record.getId(),
                "Maintenance completed. Fleet unit "
                        + "returned to ACTIVE."
        );

        return toView(record);
    }

    // cancel maintenance
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public MaintenanceRecordView cancelMaintenance(
            Long recordId) {

        MaintenanceRecord record =
                findRecord(recordId);

        record.cancel();

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
    public Page<MaintenanceRecordView> searchRecords(
            Pageable pageable) {

        return repository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toView);
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