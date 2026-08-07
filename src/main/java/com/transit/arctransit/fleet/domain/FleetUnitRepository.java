package com.transit.arctransit.fleet.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for fleet unit master records.
 *
 * Extends JpaRepository which provides built-in CRUD and pagination.
 */
public interface FleetUnitRepository extends JpaRepository<FleetUnit, Long> {

    Optional<FleetUnit> findByUnitNumber(String unitNumber);

    Optional<FleetUnit> findByPlateNumber(String plateNumber);

    /** Returns only non-archived fleet units for management list views. */
    Page<FleetUnit> findByArchivedAtIsNull(Pageable pageable);

    /** Returns only archived fleet units for the archive view. */
    Page<FleetUnit> findByArchivedAtIsNotNull(Pageable pageable);
}
