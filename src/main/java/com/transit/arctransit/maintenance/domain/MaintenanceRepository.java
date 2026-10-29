package com.transit.arctransit.maintenance.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// data access for maintenance records
public interface MaintenanceRepository
        extends JpaRepository<MaintenanceRecord, Long> {

    Page<MaintenanceRecord> findByArchivedAtIsNull(Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            SELECT record FROM MaintenanceRecord record WHERE record.archivedAt IS NOT NULL
              AND (:filter = '' OR lower(record.description) LIKE lower(concat('%', :filter, '%'))
                OR lower(coalesce(record.technicianNotes, '')) LIKE lower(concat('%', :filter, '%'))
                OR cast(record.id AS string) = :filter OR cast(record.fleetUnitId AS string) = :filter)
            """)
    Page<MaintenanceRecord> searchArchived(@org.springframework.data.repository.query.Param("filter") String filter,
            Pageable pageable);

    // newest maintenance records first
    Page<MaintenanceRecord>
    findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );

    // maintenance records for one fleet unit
    List<MaintenanceRecord>
    findByFleetUnitIdAndMaintenanceStatusNotIn(
            Long fleetUnitId,
            List<MaintenanceStatus> excludedStatuses
    );

    // count non-terminal maintenance for one fleet unit
    long countByFleetUnitIdAndMaintenanceStatusNotIn(
            Long fleetUnitId,
            List<MaintenanceStatus> excludedStatuses
    );
}
