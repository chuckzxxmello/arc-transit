package com.transit.arctransit.fleet;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// public application service contract for managing fleets
public interface FleetManagementService {

    FleetUnitView updateState(@NotNull @Valid UpdateFleetStateCommand command);

    FleetUnitView lockUnitForAssignment(@NotNull @Positive Long id);

    FleetUnitView changeAvailability(@NotNull @Valid ChangeVehicleAvailabilityCommand command);

    FleetUnitView changeAdministrativeStatus(@NotNull @Positive Long id, @NotBlank @Size(max = 50) @SafeText String status);

    Page<VehicleAvailabilityHistoryView> availabilityHistory(@NotNull @Positive Long id, Pageable pageable);

    // create new bus fleet
    FleetUnitView createUnit(@NotNull @Valid CreateFleetUnitCommand command);

    // update bus fleet unit details
    FleetUnitView updateUnit(@NotNull @Valid UpdateFleetUnitCommand command);

    // edit bus fleet unit operational status
    FleetUnitView changeStatus(@NotNull @Positive Long id, @NotBlank @Size(max = 50) @SafeText String newStatus);

    // archive / soft-delete bus fleet unit
    void archiveUnit(@NotNull @Positive Long id);

    // restore fleet unit from the archive
    void unarchiveUnit(@NotNull @Positive Long id);

    /** Permanently deletes a fleet unit from the database. */
    void hardDeleteUnit(@NotNull @Positive Long id);

    // to search active fleet units
    Page<FleetUnitSummaryView> searchUnits(@Valid FleetUnitQuery query, Pageable pageable);

    // to search archived fleet units
    Page<FleetUnitSummaryView> searchArchivedUnits(Pageable pageable);

    // obtain bus fleet unit by ID
    FleetUnitView getUnit(@NotNull @Positive Long id);
}
