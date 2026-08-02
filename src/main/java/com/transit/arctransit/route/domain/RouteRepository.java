package com.transit.arctransit.route.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for route records.
 */
public interface RouteRepository extends JpaRepository<Route, Long> {

    Optional<Route> findByRouteCode(String routeCode);

    /** Returns only non-archived routes for management list views. */
    Page<Route> findByArchivedAtIsNull(Pageable pageable);

    /** Returns only archived routes for the archive view. */
    Page<Route> findByArchivedAtIsNotNull(Pageable pageable);
}
