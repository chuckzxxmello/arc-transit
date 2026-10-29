package com.transit.arctransit.maintenance.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceHistoryRepository extends JpaRepository<MaintenanceHistory, Long> {

    Page<MaintenanceHistory> findByMaintenanceRecordIdOrderByRecordedAtDescIdDesc(
            Long maintenanceRecordId, Pageable pageable);
}