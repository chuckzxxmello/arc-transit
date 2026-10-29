package com.transit.arctransit.maintenance;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// public application contract for Maintenance
public interface MaintenanceService {

    Page<MaintenanceRecordView> searchArchivedRecords(@Size(max = 200) @SafeText String filter, Pageable pageable);

    MaintenanceRecordView getRecord(@NotNull @Positive Long id);

    Page<MaintenanceHistoryView> history(@NotNull @Positive Long id, Pageable pageable);

    // request maintenance
    MaintenanceRecordView requestMaintenance(
            @NotNull @Valid CreateMaintenanceCommand command
    );

    // approve request
    MaintenanceRecordView approveMaintenance(
            @NotNull @Positive Long recordId
    );

    // start maintenance
    MaintenanceRecordView startMaintenance(
            @NotNull @Positive Long recordId
    );

    // complete maintenance
    MaintenanceRecordView completeMaintenance(
            @NotNull @Positive Long recordId,
            @NotBlank @Size(max = 500) @SafeText(multiline = true) String technicianNotes
    );

    // cancel maintenance
    MaintenanceRecordView cancelMaintenance(
            @NotNull @Positive Long recordId
    );

    // count active maintenance for a fleet unit
    long countActiveMaintenanceForUnit(
            @NotNull @Positive Long fleetUnitId
    );

    // search maintenance records
    Page<MaintenanceRecordView> searchRecords(
            Pageable pageable
    );
}
