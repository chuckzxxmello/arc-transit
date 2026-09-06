package com.transit.arctransit.fleet;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// public application service contract for managing fleets
public interface FleetManagementService {

    // create new bus fleet
    FleetUnitView createUnit(@Valid CreateFleetUnitCommand command);

    // update bus fleet unit details
    FleetUnitView updateUnit(@Valid UpdateFleetUnitCommand command);

    // edit bus fleet unit operational status
    FleetUnitView changeStatus(Long id, String newStatus);

    // archive / soft-delete bus fleet unit
    void archiveUnit(Long id);

    // restore fleet unit from the archive
    void unarchiveUnit(Long id);

    /** Permanently deletes a fleet unit from the database. */
    void hardDeleteUnit(Long id);

    // to search active fleet units
    Page<FleetUnitSummaryView> searchUnits(FleetUnitQuery query, Pageable pageable);

    // to search archived fleet units
    Page<FleetUnitSummaryView> searchArchivedUnits(Pageable pageable);

    // obtain bus fleet unit by ID
    FleetUnitView getUnit(Long id);
}
