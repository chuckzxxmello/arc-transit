package com.transit.arctransit.route.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RouteVersionRepository extends JpaRepository<RouteVersion, Long> {

    Optional<RouteVersion> findFirstByRouteIdOrderByEffectiveFromDesc(Long routeId);

    List<RouteVersion> findByRouteIdOrderByEffectiveFromDesc(Long routeId);

    @Query("""
            SELECT version FROM RouteVersion version
            WHERE version.routeId = :routeId
              AND version.effectiveFrom <= :date
              AND (version.effectiveTo IS NULL OR version.effectiveTo > :date)
            """)
    Optional<RouteVersion> findEffective(@Param("routeId") Long routeId,
                                        @Param("date") LocalDate date);

    // Closing an interval must preserve applicability for already recorded trips.
    @Query(value = """
            SELECT EXISTS(SELECT 1 FROM arc.dispatch_assignments
              WHERE route_version_id = :versionId AND dispatch_date >= :date)
            """, nativeQuery = true)
    boolean hasAssignmentsOnOrAfter(@Param("versionId") Long versionId,
                                     @Param("date") LocalDate date);
}
