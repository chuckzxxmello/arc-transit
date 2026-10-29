package com.transit.arctransit.fleet.application;

// fleet module
import com.transit.arctransit.fleet.CreateFleetUnitCommand;
import com.transit.arctransit.fleet.ChangeVehicleAvailabilityCommand;
import com.transit.arctransit.fleet.VehicleAvailabilityHistoryView;
import com.transit.arctransit.fleet.domain.VehicleAvailability;
import com.transit.arctransit.fleet.domain.VehicleAvailabilityHistory;
import com.transit.arctransit.fleet.domain.VehicleAvailabilityHistoryRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.Instant;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitQuery;
import com.transit.arctransit.fleet.FleetUnitSummaryView;
import com.transit.arctransit.fleet.FleetUnitView;
import com.transit.arctransit.fleet.UpdateFleetUnitCommand;
import com.transit.arctransit.fleet.UpdateFleetStateCommand;

// fleet domain
import com.transit.arctransit.fleet.domain.FleetUnit;
import com.transit.arctransit.fleet.domain.AdministrativeStatus;
import com.transit.arctransit.fleet.domain.FleetUnitRepository;
import com.transit.arctransit.fleet.domain.OperationalStatus;
import com.transit.arctransit.fleet.domain.VehicleType;

//common / audit
import com.transit.arctransit.audit.AuditRecordingService;
import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


// app_service for fleet management
@Service
@Validated
@Transactional
public class AppFleetManagementService
        implements FleetManagementService {

    private static final Logger log =
            LoggerFactory.getLogger(AppFleetManagementService.class
    );

    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";
    private static final String OPERATIONS_STAFF_ROLE = "OPERATIONS_STAFF";

    private final FleetUnitRepository fleetUnitRepository;
    private final VehicleAvailabilityHistoryRepository historyRepository;
    private final AuditRecordingService auditService;

    public AppFleetManagementService(FleetUnitRepository fleetUnitRepository,
            AuditRecordingService auditService,
            VehicleAvailabilityHistoryRepository historyRepository) {
    	
        this.fleetUnitRepository = fleetUnitRepository;
        this.auditService = auditService;
        this.historyRepository = historyRepository;
    }

    //	create bus fleet unit
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public FleetUnitView createUnit(
            CreateFleetUnitCommand command) {

        String unitNumber = normalizeIdentifier(command.unitNumber());
        String plateNumber = normalizeIdentifier(command.plateNumber());

        if (fleetUnitRepository
                .existsByUnitNumber(unitNumber)) {

            throw new BusinessConflictException("Unit number is already in use: "
            															+ unitNumber
            );
        }

        if (fleetUnitRepository
                .existsByPlateNumber(plateNumber)) {

            throw new BusinessConflictException("Plate number is already in use: "
            															+ plateNumber
            );
        }

        FleetUnit unit = new FleetUnit(
                unitNumber,
                plateNumber,
                VehicleType.BUS,
                command.capacity()
        );

        FleetUnit saved = fleetUnitRepository.save(unit);
        historyRepository.save(new VehicleAvailabilityHistory(
                saved.getId(), null, saved.getAvailability().name(),
                saved.getAvailabilityEffectiveAt(), Instant.now(), currentActor(),
                "REGISTRATION", "Availability is unknown until reported by staff."));

        auditService.recordAction(
				                "FLEET_UNIT_REGISTERED",
				                "FleetUnit",
				                saved.getId(),
				                "Registered unit " + saved.getUnitNumber()
        );

        log.info(
                "Registered fleet unit '{}'.",
                saved.getUnitNumber()
        );

        return toView(saved);
    }

    // update bus unit
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public FleetUnitView updateUnit(
            UpdateFleetUnitCommand command) {

        FleetUnit unit = fleetUnitRepository.findLockedById(command.id())
                        .orElseThrow(() ->
                        new ResourceNotFoundException("Fleet unit not found: "
                                        					  			+ command.id()
                        ));

        String unitNumber = normalizeIdentifier(command.unitNumber());
        String plateNumber = normalizeIdentifier(command.plateNumber());

        if (fleetUnitRepository.existsByUnitNumberAndIdNot(
								                           unitNumber,
								                           unit.getId()
                )) {

            throw new BusinessConflictException("Unit number is already in use: "
							                    						+ unitNumber
            );
        }

        if (fleetUnitRepository.existsByPlateNumberAndIdNot(
									                        plateNumber,
									                        unit.getId()
                )) {

            throw new BusinessConflictException("Plate number is already in use: "
            									+ plateNumber
            );
        }

        unit.updateDetails(
		                   unitNumber,
		                   plateNumber,
		                   command.capacity()
        );

        auditService.recordAction(
				                  "FLEET_UNIT_UPDATED",
				                  "FleetUnit",
				                  unit.getId(),
				                  "Updated details for unit " + unit.getUnitNumber()
        );

        log.info(
                "Updated fleet unit '{}'.",
                unit.getUnitNumber()
        );

        return toView(unit);
    }

    // change the operational status of the bus fleet unit
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public FleetUnitView changeStatus(
							          Long id,
							          String newStatus) {

        FleetUnit unit = fleetUnitRepository.findLockedById(id)
        						.orElseThrow(() ->
                                new ResourceNotFoundException("Fleet unit not found: " + id
        ));

        if (unit.isArchived()) {
            throw new CommandValidationException("Restore the archived fleet unit before changing its state.");
        }
        OperationalStatus status = parseOperationalStatus(newStatus);

        String previous = unit.getAvailability().name();
        unit.changeOperationalStatus(status);
        historyRepository.save(new VehicleAvailabilityHistory(
                unit.getId(), previous, unit.getAvailability().name(),
                unit.getAvailabilityEffectiveAt(), Instant.now(), currentActor(),
                "LEGACY_STATUS_CHANGE", "Recorded legacy status " + status.name()));

        auditService.recordAction(
				                  "FLEET_UNIT_STATUS_CHANGED",
				                  "FleetUnit",
				                  unit.getId(),
				                  "Status changed to "
					                        + status.name()
					                        + " for unit "
					                        + unit.getUnitNumber()
        );

        log.info("Changed fleet unit '{}' status to '{}'.",
								                 unit.getUnitNumber(),
								                 status
        );
        return toView(unit);
    }

    @Override
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public FleetUnitView changeAdministrativeStatus(Long id, String value) {
        FleetUnit unit = findLocked(id);
        if (unit.isArchived()) {
            throw new CommandValidationException("Restore the archived fleet unit before changing its administrative state.");
        }
        AdministrativeStatus status;
        try {
            status = AdministrativeStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new CommandValidationException("Administrative status must be ACTIVE or INACTIVE.");
        }
        unit.changeAdministrativeStatus(status);
        auditService.recordAction("FLEET_ADMINISTRATIVE_STATUS_CHANGED", "FleetUnit",
                id, "Administrative status changed to " + status.name());
        return toView(unit);
    }

    @Override
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public FleetUnitView updateState(UpdateFleetStateCommand command) {
        FleetUnit unit = findLocked(command.fleetUnitId());
        if (unit.isArchived()) {
            throw new CommandValidationException("Restore the archived bus before changing its state.");
        }
        AdministrativeStatus administrative;
        VehicleAvailability availability;
        try {
            administrative = AdministrativeStatus.valueOf(command.administrativeStatus().trim().toUpperCase(Locale.ROOT));
            availability = VehicleAvailability.valueOf(command.availability().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new CommandValidationException("Select a valid administrative status and reported availability.");
        }
        boolean availabilityChanged = availability != unit.getAvailability();
        if (availabilityChanged && (command.reason() == null || command.reason().isBlank())) {
            throw new CommandValidationException("A reason is required when reported availability changes.");
        }
        if (administrative != unit.getAdministrativeStatus()) {
            changeAdministrativeStatus(unit.getId(), administrative.name());
        }
        if (availabilityChanged) {
            changeAvailability(new ChangeVehicleAvailabilityCommand(unit.getId(), availability.name(),
                    Instant.now(), command.reason()));
        }
        return toView(unit);
    }

    // archive teh bus fleet unit
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void archiveUnit(Long id) {

        FleetUnit unit = fleetUnitRepository.findLockedById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Fleet unit not found: " + id
        ));

        unit.archive();

        auditService.recordAction(
				                  "FLEET_UNIT_ARCHIVED",
				                  "FleetUnit",
				                  id,
				                  "Archived unit "
				                        + unit.getUnitNumber()
        );

        log.info("Archived fleet unit '{}'.",
                				unit.getUnitNumber()
        );
    }

    // un-archive the archived bus fleet unit
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void unarchiveUnit(Long id) {

        FleetUnit unit = fleetUnitRepository.findLockedById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(
							                                     "Fleet unit not found: " + id
        ));
        unit.unarchive();

        auditService.recordAction(
				                  "FLEET_UNIT_UNARCHIVED",
				                  "FleetUnit",
				                  id,
				                  "Unarchived unit "
				                          + unit.getUnitNumber()
        );

        log.info(
                 "Unarchived fleet unit '{}'.",
                 unit.getUnitNumber()
        );
    }

    // permanently delete the bus fleet unit data
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void hardDeleteUnit(Long id) {
        FleetUnit unit = fleetUnitRepository.findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet unit not found: " + id));
        if (!unit.isArchived()) {
            throw new BusinessConflictException("Archive the bus before permanently deleting it.");
        }
        var blockers = fleetUnitRepository.findPermanentDeletionBlockers(id);
        if (!blockers.isEmpty()) {
            throw new BusinessConflictException("Cannot permanently delete this bus because it has "
                    + String.join(", ", blockers) + ". Keep it archived to preserve those records.");
        }
        try {
            fleetUnitRepository.delete(unit);
            fleetUnitRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessConflictException("The bus has linked records and could not be deleted. Refresh and try again.");
        }
        auditService.recordAction("FLEET_UNIT_DELETED", "FleetUnit", id,
                "Permanently deleted unit " + unit.getUnitNumber() + " (plate " + unit.getPlateNumber()
                        + "). Availability history retained under fleet ID " + id + ".");
        log.info("Permanently deleted fleet unit '{}'.", unit.getUnitNumber());
    }

    // search for active bus fleet units
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public Page<FleetUnitSummaryView> searchUnits(
									            FleetUnitQuery query,
									            Pageable pageable) {

        String searchFilter = normalizeSearchFilter(
						                        query == null
						                                ? null
						                                : query.unitOrPlateFilter()
        );

        OperationalStatus status = parseOptionalOperationalStatus(
									                        query == null
									                                ? null
									                                : query.statusFilter()
                );

        return fleetUnitRepository.searchActive(
					                        searchFilter,
					                        status,
					                        pageable
					                )
					                .map(this::toSummaryView);
    }

    // search for bus fleet unit in the archive 
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public Page<FleetUnitSummaryView> searchArchivedUnits(
    		
            Pageable pageable) {
					        return fleetUnitRepository
								                .findByArchivedAtIsNotNull(pageable)
								                .map(this::toSummaryView);
    		}

    // get bus fleet unit id
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public FleetUnitView getUnit(Long id) {

        FleetUnit unit = fleetUnitRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
					                                        "Fleet unit not found: " + id
        ));

        return toView(unit);
    }

    @Override
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public FleetUnitView lockUnitForAssignment(Long id) {
        return toView(findLocked(id));
    }

    @Override
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public FleetUnitView changeAvailability(ChangeVehicleAvailabilityCommand command) {
        FleetUnit unit = findLocked(command.fleetUnitId());
        if (unit.isArchived()) {
            throw new CommandValidationException("Archived fleet units cannot receive availability changes.");
        }
        VehicleAvailability availability;
        try {
            availability = VehicleAvailability.valueOf(command.availability().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new CommandValidationException("Invalid availability.");
        }
        Instant recordedAt = Instant.now();
        Instant effectiveAt = command.effectiveAt();
        if (effectiveAt == null || effectiveAt.isAfter(recordedAt)) {
            throw new CommandValidationException("Effective time is required and cannot be in the future.");
        }
        if (effectiveAt.isBefore(unit.getAvailabilityEffectiveAt())) {
            throw new CommandValidationException("Effective time precedes the current availability report.");
        }
        String reason = command.reason();
        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw new CommandValidationException("A reason of at most 500 characters is required.");
        }
        String previous = unit.getAvailability().name();
        unit.changeAvailability(availability, effectiveAt);
        historyRepository.save(new VehicleAvailabilityHistory(
                unit.getId(), previous, availability.name(), effectiveAt, recordedAt,
                currentActor(), "STAFF_ENTRY", reason.trim()));
        auditService.recordAction("FLEET_AVAILABILITY_CHANGED", "FleetUnit",
                unit.getId(), "Recorded availability " + availability.name());
        return toView(unit);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public Page<VehicleAvailabilityHistoryView> availabilityHistory(Long id, Pageable pageable) {
        return historyRepository.findByFleetUnitIdOrderByRecordedAtDescIdDesc(id, pageable)
                .map(event -> new VehicleAvailabilityHistoryView(
                        event.getId(), event.getPreviousAvailability(), event.getAvailability(),
                        event.getEffectiveAt(), event.getRecordedAt(), event.getRecordedBy(),
                        event.getSourceType(), event.getVerificationStatus(), event.getReason()));
    }

    private FleetUnit findLocked(Long id) {
        return fleetUnitRepository.findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FleetUnit not found: " + id));
    }

    private static String currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "SYSTEM" : authentication.getName();
    }

    // fleet unit view
    private FleetUnitView toView(
            FleetUnit unit) {

        return new FleetUnitView(
                unit.getId(),
                unit.getUnitNumber(),
                unit.getPlateNumber(),
                unit.getVehicleType().name(),
                unit.getCapacity(),
                unit.getOperationalStatus().name(),
                unit.isArchived() ? "ARCHIVED" : unit.getAdministrativeStatus().name(),
                unit.getAvailability().name(),
                unit.getArchivedAt()
        );
    }

    private FleetUnitSummaryView toSummaryView(
            FleetUnit unit) {

        return new FleetUnitSummaryView(
                unit.getId(),
                unit.getUnitNumber(),
                unit.getPlateNumber(),
                unit.getCapacity(),
                unit.getOperationalStatus().name(),
                unit.isArchived() ? "ARCHIVED" : unit.getAdministrativeStatus().name(),
                unit.getAvailability().name(),
                unit.getArchivedAt()
        );
    }

    // normalization and validation of bus fleet units
    private static String normalizeIdentifier(String value) {

        if (value == null || value.isBlank()) {
            throw new CommandValidationException("Fleet identifier cannot be blank.");
        }
        return value
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private static String normalizeSearchFilter(String value) {

        if (value == null) {
            return null;
        }
        String normalized = value.trim();

        return normalized.isBlank()
				                ? null
				                : normalized;
    }

    private static OperationalStatus parseOperationalStatus(String value) {

        if (value == null || value.isBlank()) {
            throw new CommandValidationException("Operational status cannot be blank.");
        }

        try {
            return OperationalStatus.valueOf(
						                    value
						                        .trim()
						                        .toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException exception) {

            throw new CommandValidationException("Invalid operational status: "
                            												+ value
            );
        }
    }

    private static OperationalStatus
    							parseOptionalOperationalStatus(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return parseOperationalStatus(value);
    }
}
