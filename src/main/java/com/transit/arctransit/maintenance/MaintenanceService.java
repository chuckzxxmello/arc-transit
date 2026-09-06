package com.transit.arctransit.maintenance;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// public application contract for Maintenance
public interface MaintenanceService {

    // request maintenance
    MaintenanceRecordView requestMaintenance(
            @Valid CreateMaintenanceCommand command
    );

    // approve request
    MaintenanceRecordView approveMaintenance(
            Long recordId
    );

    // start maintenance
    MaintenanceRecordView startMaintenance(
            Long recordId
    );

    // complete maintenance
    MaintenanceRecordView completeMaintenance(
            Long recordId,
            String technicianNotes
    );

    // cancel maintenance
    MaintenanceRecordView cancelMaintenance(
            Long recordId
    );

    // count active maintenance for a fleet unit
    long countActiveMaintenanceForUnit(
            Long fleetUnitId
    );

    // search maintenance records
    Page<MaintenanceRecordView> searchRecords(
            Pageable pageable
    );
}