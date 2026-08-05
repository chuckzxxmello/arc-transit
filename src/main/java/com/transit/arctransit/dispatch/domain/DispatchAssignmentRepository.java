package com.transit.arctransit.dispatch.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * Finds all non-cancelled, non-archived assignments for a fleet unit on a given date.
     * Used to check for double-booking a bus.
     */
    @Query("SELECT da FROM DispatchAssignment da WHERE da.fleetUnitId = :fleetUnitId AND da.dispatchDate = :date AND da.dispatchStatus <> :excludeStatus AND da.archivedAt IS NULL")
    List<DispatchAssignment> findByFleetUnitIdAndDispatchDateAndDispatchStatusNot(
            @Param("fleetUnitId") Long fleetUnitId, @Param("date") LocalDate date, @Param("excludeStatus") DispatchStatus excludeStatus);

    /**
     * Finds all non-cancelled, non-archived assignments for a driver on a given date.
     * Used to check for double-booking a driver.
     */
    @Query("SELECT da FROM DispatchAssignment da WHERE da.driverId = :driverId AND da.dispatchDate = :date AND da.dispatchStatus <> :excludeStatus AND da.archivedAt IS NULL")
    List<DispatchAssignment> findByDriverIdAndDispatchDateAndDispatchStatusNot(
            @Param("driverId") Long driverId, @Param("date") LocalDate date, @Param("excludeStatus") DispatchStatus excludeStatus);

    /** Paginated search for all non-archived assignments. */
    Page<DispatchAssignment> findByArchivedAtIsNull(Pageable pageable);

    /** Paginated search for all archived assignments. */
    Page<DispatchAssignment> findByArchivedAtIsNotNull(Pageable pageable);

    /** Counts assignments by status that are not archived (used for dashboard cards). */
    long countByDispatchStatusAndArchivedAtIsNull(DispatchStatus status);
}
