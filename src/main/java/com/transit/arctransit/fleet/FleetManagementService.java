package com.transit.arctransit.fleet;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Public application service contract for Fleet Management.
 *
 * This interface is placed in the fleet root package so it forms
 * the public API of the Fleet module as enforced by Spring Modulith.
 * Other modules (like dispatch) can depend on this interface but
 * cannot access the internal application/ or domain/ subpackages.
 *
 * Source: https://docs.spring.io/spring-modulith/reference/fundamentals.html
 * (Ctrl+F: "API package")
 */
public interface FleetManagementService {

    /** Registers a new bus in the fleet. */
    FleetUnitView createUnit(@Valid CreateFleetUnitCommand command);

    /** Updates an existing fleet unit's details. */
    FleetUnitView updateUnit(@Valid UpdateFleetUnitCommand command);

    /** Changes a fleet unit's operational status. */
    FleetUnitView changeStatus(Long id, String newStatus);

    /** Soft-deletes a fleet unit by setting its archive timestamp. */
    void archiveUnit(Long id);

    /** Restores a fleet unit from the archive. */
    void unarchiveUnit(Long id);

    /** Permanently deletes a fleet unit from the database. */
    void hardDeleteUnit(Long id);

    /** Searches active fleet units for management list views. */
    Page<FleetUnitSummaryView> searchUnits(FleetUnitQuery query, Pageable pageable);

    /** Searches soft-deleted fleet units for the archive view. */
    Page<FleetUnitSummaryView> searchArchivedUnits(Pageable pageable);

    /** Retrieves a single fleet unit by ID. */
    FleetUnitView getUnit(Long id);
}
