package com.transit.arctransit.fleet.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleAvailabilityHistoryRepository
        extends JpaRepository<VehicleAvailabilityHistory, Long> {

    Page<VehicleAvailabilityHistory> findByFleetUnitIdOrderByRecordedAtDescIdDesc(
            Long fleetUnitId, Pageable pageable);
}