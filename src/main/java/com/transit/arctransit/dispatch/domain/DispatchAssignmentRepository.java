package com.transit.arctransit.dispatch.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface DispatchAssignmentRepository extends JpaRepository<DispatchAssignment, Long> {

    @Query(value = """
            SELECT assignment.* FROM arc.dispatch_assignments assignment
            JOIN arc.fleet_units fleet ON fleet.id = assignment.fleet_unit_id
            JOIN arc.drivers driver ON driver.id = assignment.driver_id
            JOIN arc.routes route ON route.id = assignment.route_id
            WHERE assignment.archived_at IS NULL AND lower(concat('Dispatch #', assignment.id, ' ',
                assignment.dispatch_date, ' ', fleet.unit_number, ' ', driver.first_name, ' ',
                driver.last_name, ' ', route.route_code)) LIKE lower(concat('%', :filter, '%'))
            """, countQuery = """
            SELECT count(*) FROM arc.dispatch_assignments assignment
            JOIN arc.fleet_units fleet ON fleet.id = assignment.fleet_unit_id
            JOIN arc.drivers driver ON driver.id = assignment.driver_id
            JOIN arc.routes route ON route.id = assignment.route_id
            WHERE assignment.archived_at IS NULL AND lower(concat('Dispatch #', assignment.id, ' ',
                assignment.dispatch_date, ' ', fleet.unit_number, ' ', driver.first_name, ' ',
                driver.last_name, ' ', route.route_code)) LIKE lower(concat('%', :filter, '%'))
            """, nativeQuery = true)
    Page<DispatchAssignment> searchReportCandidates(@Param("filter") String filter, Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT assignment FROM DispatchAssignment assignment WHERE assignment.id = :id")
    java.util.Optional<DispatchAssignment> findLockedById(@Param("id") Long id);

    @Query(value = """
            SELECT 'incidents' WHERE EXISTS (
                SELECT 1 FROM arc.incidents WHERE affected_service_type = 'DISPATCH' AND affected_entity_id = :id)
            UNION ALL SELECT 'operational reports' WHERE EXISTS (
                SELECT 1 FROM arc.operational_reports WHERE entity_type = 'DISPATCH' AND entity_id = :id)
            UNION ALL SELECT 'reconciliation exceptions' WHERE EXISTS (
                SELECT 1 FROM arc.operational_exceptions WHERE affected_entity_type = 'DISPATCH' AND affected_entity_id = :id)
            """, nativeQuery = true)
    List<String> findPermanentDeletionBlockers(@Param("id") Long id);

    @Query("""
            SELECT assignment FROM DispatchAssignment assignment
            WHERE assignment.fleetUnitId = :fleetUnitId
              AND assignment.dispatchStatus <> com.transit.arctransit.dispatch.domain.DispatchStatus.CANCELLED
              AND (:excludeId IS NULL OR assignment.id <> :excludeId)
              AND assignment.scheduledDeparture < :end
              AND (assignment.scheduledArrival > :start
                   OR (assignment.scheduledArrival IS NULL AND (
                       assignment.dispatchStatus <> com.transit.arctransit.dispatch.domain.DispatchStatus.COMPLETED
                       OR assignment.actualArrival IS NULL
                       OR assignment.actualArrival <= assignment.scheduledDeparture
                       OR assignment.actualArrival > :start)))
            """)
    List<DispatchAssignment> findFleetOverlaps(@Param("fleetUnitId") Long fleetUnitId,
            @Param("start") Instant start, @Param("end") Instant end,
            @Param("excludeId") Long excludeId);

    @Query("""
            SELECT assignment FROM DispatchAssignment assignment
            WHERE assignment.driverId = :driverId
              AND assignment.dispatchStatus <> com.transit.arctransit.dispatch.domain.DispatchStatus.CANCELLED
              AND (:excludeId IS NULL OR assignment.id <> :excludeId)
              AND assignment.scheduledDeparture < :end
              AND (assignment.scheduledArrival > :start
                   OR (assignment.scheduledArrival IS NULL AND (
                       assignment.dispatchStatus <> com.transit.arctransit.dispatch.domain.DispatchStatus.COMPLETED
                       OR assignment.actualArrival IS NULL
                       OR assignment.actualArrival <= assignment.scheduledDeparture
                       OR assignment.actualArrival > :start)))
            """)
    List<DispatchAssignment> findDriverOverlaps(@Param("driverId") Long driverId,
            @Param("start") Instant start, @Param("end") Instant end,
            @Param("excludeId") Long excludeId);

    @Query("""
            SELECT assignment FROM DispatchAssignment assignment
            WHERE assignment.archivedAt IS NULL
              AND (:date IS NULL OR assignment.dispatchDate = :date)
              AND (:status IS NULL OR assignment.dispatchStatus = :status)
            """)
    Page<DispatchAssignment> search(@Param("date") LocalDate date,
            @Param("status") DispatchStatus status, Pageable pageable);

    boolean existsByFleetUnitIdAndDispatchStatusAndIdNot(
            Long fleetUnitId, DispatchStatus status, Long excludeId);

    boolean existsByDriverIdAndDispatchStatusAndIdNot(
            Long driverId, DispatchStatus status, Long excludeId);

    Page<DispatchAssignment> findByArchivedAtIsNull(Pageable pageable);
    Page<DispatchAssignment> findByArchivedAtIsNotNull(Pageable pageable);
    long countByDispatchStatusAndArchivedAtIsNull(DispatchStatus status);
}
