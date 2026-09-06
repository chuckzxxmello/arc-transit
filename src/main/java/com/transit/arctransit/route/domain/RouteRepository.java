package com.transit.arctransit.route.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// data access for route records
public interface RouteRepository
        extends JpaRepository<Route, Long> {

    // lookup by route code
    Optional<Route> findByRouteCode(
            String routeCode
    );

    // return active routes without archive timestamp
    Page<Route> findByArchivedAtIsNull(
            Pageable pageable
    );

    // return archived routes
    Page<Route> findByArchivedAtIsNotNull(
            Pageable pageable
    );

    // database-backed search for active routes
    @Query("""
            SELECT route
            FROM Route route
            WHERE route.archivedAt IS NULL
              AND (
                    :searchFilter IS NULL
                    OR :searchFilter = ''
                    OR LOWER(route.routeCode)
                       LIKE LOWER(
                           CONCAT('%', :searchFilter, '%')
                       )
                    OR LOWER(route.routeName)
                       LIKE LOWER(
                           CONCAT('%', :searchFilter, '%')
                       )
              )
              AND (
                    :status IS NULL
                    OR route.routeStatus = :status
              )
            """)
    Page<Route> searchActive(

            @Param("searchFilter")
            String searchFilter,

            @Param("status")
            RouteStatus status,

            Pageable pageable

    );
}