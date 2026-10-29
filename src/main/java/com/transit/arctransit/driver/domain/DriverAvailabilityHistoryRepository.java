package com.transit.arctransit.driver.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverAvailabilityHistoryRepository
        extends JpaRepository<DriverAvailabilityHistory, Long> {

    Page<DriverAvailabilityHistory> findByDriverIdOrderByRecordedAtDescIdDesc(
            Long driverId, Pageable pageable);
}