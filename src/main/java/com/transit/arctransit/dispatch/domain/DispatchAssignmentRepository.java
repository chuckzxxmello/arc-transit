package com.transit.arctransit.dispatch.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Data access for dispatch assignment records.
 *
 * The overlap-checking methods support the dispatch service's
 * conflict validation logic.
 */
public interface DispatchAssignmentRepository extends JpaRepository<DispatchAssignment, Long> {

    /**
     * Finds all non-cancelled assignments for a fleet unit on a given date.
     * Used to check for double-booking a bus.
     */
    List<DispatchAssignment> findByFleetUnitIdAndDispatchDateAndDispatchStatusNot(
            Long fleetUnitId, LocalDate date, DispatchStatus excludeStatus);

    /**
     * Finds all non-cancelled assignments for a driver on a given date.
     * Used to check for double-booking a driver.
     */
    List<DispatchAssignment> findByDriverIdAndDispatchDateAndDispatchStatusNot(
            Long driverId, LocalDate date, DispatchStatus excludeStatus);

    /** Paginated search for all assignments. */
    Page<DispatchAssignment> findAll(Pageable pageable);
}
