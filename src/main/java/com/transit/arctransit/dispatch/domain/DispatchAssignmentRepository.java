package com.transit.arctransit.dispatch.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

// data access for dispatch assignments
public interface DispatchAssignmentRepository
        extends JpaRepository<DispatchAssignment, Long> {

    // fleet overlap detection
    @Query("""
            SELECT assignment
            FROM DispatchAssignment assignment
            WHERE assignment.fleetUnitId = :fleetUnitId
              AND assignment.dispatchDate = :date
              AND assignment.dispatchStatus <> :excludeStatus
              AND assignment.archivedAt IS NULL
            """)
    List<DispatchAssignment>
    findByFleetUnitIdAndDispatchDateAndDispatchStatusNot(
            @Param("fleetUnitId")
            Long fleetUnitId,

            @Param("date")
            LocalDate date,

            @Param("excludeStatus")
            DispatchStatus excludeStatus
    );

    // driver overlap detection
    @Query("""
            SELECT assignment
            FROM DispatchAssignment assignment
            WHERE assignment.driverId = :driverId
              AND assignment.dispatchDate = :date
              AND assignment.dispatchStatus <> :excludeStatus
              AND assignment.archivedAt IS NULL
            """)
    List<DispatchAssignment>
    findByDriverIdAndDispatchDateAndDispatchStatusNot(
            @Param("driverId")
            Long driverId,

            @Param("date")
            LocalDate date,

            @Param("excludeStatus")
            DispatchStatus excludeStatus
    );

    // active dispatch assignments
    Page<DispatchAssignment>
    findByArchivedAtIsNull(
            Pageable pageable
    );

    // archived dispatch assignments
    Page<DispatchAssignment>
    findByArchivedAtIsNotNull(
            Pageable pageable
    );

    // dashboard count
    long countByDispatchStatusAndArchivedAtIsNull(
            DispatchStatus status
    );
}