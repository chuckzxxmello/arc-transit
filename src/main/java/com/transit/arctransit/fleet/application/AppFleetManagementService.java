package com.transit.arctransit.fleet.application;

// fleet module
import com.transit.arctransit.fleet.CreateFleetUnitCommand;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitQuery;
import com.transit.arctransit.fleet.FleetUnitSummaryView;
import com.transit.arctransit.fleet.FleetUnitView;
import com.transit.arctransit.fleet.UpdateFleetUnitCommand;

// fleet domain
import com.transit.arctransit.fleet.domain.FleetUnit;
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
@Transactional
public class AppFleetManagementService
        implements FleetManagementService {

    private static final Logger log =
            LoggerFactory.getLogger(AppFleetManagementService.class
    );

    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";
    private static final String OPERATIONS_STAFF_ROLE = "OPERATIONS_STAFF";

    private final FleetUnitRepository fleetUnitRepository;
    private final AuditRecordingService auditService;

    public AppFleetManagementService(FleetUnitRepository fleetUnitRepository,
            						 AuditRecordingService auditService) {
    	
        this.fleetUnitRepository = fleetUnitRepository;
        this.auditService = auditService;
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

        FleetUnit unit = fleetUnitRepository.findById(command.id())
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

        FleetUnit unit = fleetUnitRepository.findById(id)
        						.orElseThrow(() ->
                                new ResourceNotFoundException("Fleet unit not found: " + id
        ));

        OperationalStatus status = parseOperationalStatus(newStatus);

        unit.changeOperationalStatus(status);

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

    // archive teh bus fleet unit
    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void archiveUnit(Long id) {

        FleetUnit unit = fleetUnitRepository.findById(id)
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

        FleetUnit unit = fleetUnitRepository.findById(id)
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

        FleetUnit unit = fleetUnitRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Fleet unit not found: "
                                                									+ id
        ));

        try {
            fleetUnitRepository.delete(unit);
            fleetUnitRepository.flush();

        } catch (DataIntegrityViolationException exception) {

            throw new BusinessConflictException("Cannot permanently delete fleet unit "
								                            + id
								                            + " because it is referenced by "
								                            + "other records."
            );
        }

        log.info(
                "Permanently deleted fleet unit '{}'.",
                unit.getUnitNumber()
        );
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

    // fleet unit view
    private FleetUnitView toView(
            FleetUnit unit) {

        return new FleetUnitView(
                unit.getId(),
                unit.getUnitNumber(),
                unit.getPlateNumber(),
                unit.getVehicleType().name(),
                unit.getCapacity(),
                unit.getOperationalStatus().name()
        );
    }

    private FleetUnitSummaryView toSummaryView(
            FleetUnit unit) {

        return new FleetUnitSummaryView(
                unit.getId(),
                unit.getUnitNumber(),
                unit.getPlateNumber(),
                unit.getCapacity(),
                unit.getOperationalStatus().name()
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