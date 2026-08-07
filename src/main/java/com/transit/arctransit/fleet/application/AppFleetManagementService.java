package com.transit.arctransit.fleet.application;

import com.transit.arctransit.audit.AuditRecordingService;
import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;
import com.transit.arctransit.fleet.*;
import com.transit.arctransit.fleet.domain.FleetUnit;
import com.transit.arctransit.fleet.domain.FleetUnitRepository;
import com.transit.arctransit.fleet.domain.OperationalStatus;
import com.transit.arctransit.fleet.domain.VehicleType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class resides in the fleet.application subpackage, making it
 * internal to the Fleet module per Spring Modulith conventions.
 * Other modules can only access the public FleetManagementService interface.
 *
 * All methods require authentication (either SYSTEM_ADMIN or OPERATIONS_STAFF).
 * (@PreAuthorize)
 */
@Service
@Transactional
public class AppFleetManagementService implements FleetManagementService {

    private final FleetUnitRepository fleetUnitRepository;
    private final AuditRecordingService auditService;

    /**
     * Implicit constructor injection (no @Autowired needed).
     * Spring automatically injects the single constructor's parameters.
     */
    public AppFleetManagementService(FleetUnitRepository fleetUnitRepository, AuditRecordingService auditService) {
        this.fleetUnitRepository = fleetUnitRepository;
        this.auditService = auditService;
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public FleetUnitView createUnit(@Valid CreateFleetUnitCommand command) {
        /*
         * Normalize input to uppercase and trimmed before checking uniqueness.
         * The database constraints (ck_fleet_unit_number_normalized) provide
         * the final safety boundary, but normalizing here gives better error
         * messages from the Java layer.
         */
        String normalizedUnitNumber = command.unitNumber().trim().toUpperCase();
        String normalizedPlateNumber = command.plateNumber().trim().toUpperCase();

        if (fleetUnitRepository.findByUnitNumber(normalizedUnitNumber).isPresent()) {
            throw new BusinessConflictException("Unit number is already in use: " + normalizedUnitNumber);
        }
        if (fleetUnitRepository.findByPlateNumber(normalizedPlateNumber).isPresent()) {
            throw new BusinessConflictException("Plate number is already in use: " + normalizedPlateNumber);
        }

        FleetUnit unit = new FleetUnit(
                normalizedUnitNumber,
                normalizedPlateNumber,
                VehicleType.BUS,
                command.capacity());

        FleetUnit saved = fleetUnitRepository.save(unit);
        auditService.recordAction("FLEET_UNIT_REGISTERED", "FleetUnit", saved.getId(),
                "Registered unit " + saved.getUnitNumber());
        return toView(saved);
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public FleetUnitView updateUnit(@Valid UpdateFleetUnitCommand command) {
        FleetUnit unit = fleetUnitRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("Fleet unit not found: " + command.id()));

        String normalizedUnitNumber = command.unitNumber().trim().toUpperCase();
        String normalizedPlateNumber = command.plateNumber().trim().toUpperCase();

        /*
         * Check uniqueness only if the value actually changed.
         * This prevents false conflict errors when updating other fields
         * while keeping the same unit or plate number.
         */
        fleetUnitRepository.findByUnitNumber(normalizedUnitNumber)
                .filter(existing -> !existing.getId().equals(unit.getId()))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("Unit number is already in use: " + normalizedUnitNumber);
                });

        fleetUnitRepository.findByPlateNumber(normalizedPlateNumber)
                .filter(existing -> !existing.getId().equals(unit.getId()))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("Plate number is already in use: " + normalizedPlateNumber);
                });

        unit.updateDetails(normalizedUnitNumber, normalizedPlateNumber, command.capacity());
        auditService.recordAction("FLEET_UNIT_UPDATED", "FleetUnit", unit.getId(),
                "Updated details for unit " + unit.getUnitNumber());
        return toView(unit);
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public FleetUnitView changeStatus(Long id, String newStatus) {
        FleetUnit unit = fleetUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet unit not found: " + id));

        try {
            OperationalStatus status = OperationalStatus.valueOf(newStatus);
            unit.changeOperationalStatus(status);
        } catch (IllegalArgumentException e) {
            throw new CommandValidationException("Invalid operational status: " + newStatus);
        }

        auditService.recordAction("FLEET_UNIT_STATUS_CHANGED", "FleetUnit", unit.getId(),
                "Status changed to " + newStatus + " for unit " + unit.getUnitNumber());
        return toView(unit);
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void archiveUnit(Long id) {
        FleetUnit unit = fleetUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet unit not found: " + id));
        unit.archive();
        auditService.recordAction("FLEET_UNIT_ARCHIVED", "FleetUnit", id, "Archived unit " + unit.getUnitNumber());
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void unarchiveUnit(Long id) {
        FleetUnit unit = fleetUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet unit not found: " + id));
        unit.unarchive();
        auditService.recordAction("FLEET_UNIT_UNARCHIVED", "FleetUnit", id, "Unarchived unit " + unit.getUnitNumber());
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void hardDeleteUnit(Long id) {
        try {
            fleetUnitRepository.deleteById(id);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new BusinessConflictException("Cannot permanently delete fleet unit " + id
                    + " because it is referenced by other records (e.g., dispatch assignments).");
        }
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public Page<FleetUnitSummaryView> searchUnits(FleetUnitQuery query, Pageable pageable) {
        return fleetUnitRepository.findByArchivedAtIsNull(pageable)
                .map(unit -> new FleetUnitSummaryView(
                        unit.getId(),
                        unit.getUnitNumber(),
                        unit.getPlateNumber(),
                        unit.getCapacity(),
                        unit.getOperationalStatus().name()));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public Page<FleetUnitSummaryView> searchArchivedUnits(Pageable pageable) {
        return fleetUnitRepository.findByArchivedAtIsNotNull(pageable)
                .map(unit -> new FleetUnitSummaryView(
                        unit.getId(),
                        unit.getUnitNumber(),
                        unit.getPlateNumber(),
                        unit.getCapacity(),
                        unit.getOperationalStatus().name()));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public FleetUnitView getUnit(Long id) {
        FleetUnit unit = fleetUnitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet unit not found: " + id));
        return toView(unit);
    }

    private FleetUnitView toView(FleetUnit unit) {
        return new FleetUnitView(
                unit.getId(),
                unit.getUnitNumber(),
                unit.getPlateNumber(),
                unit.getVehicleType().name(),
                unit.getCapacity(),
                unit.getOperationalStatus().name());
    }
}
