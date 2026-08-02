package com.transit.arctransit.route;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Public application service contract for Route Management.
 */
public interface RouteManagementService {

    RouteView createRoute(@Valid CreateRouteCommand command);

    RouteView updateRoute(@Valid UpdateRouteCommand command);

    RouteView changeRouteStatus(Long id, String newStatus);

    /** Soft-deletes a route. */
    void archiveRoute(Long id);

    /** Restores a route from the archive. */
    void unarchiveRoute(Long id);

    /** Permanently deletes a route from the database. */
    void hardDeleteRoute(Long id);

    /** Searches active routes for management list views. */
    Page<RouteSummaryView> searchRoutes(RouteQuery query, Pageable pageable);

    /** Searches soft-deleted routes for the archive view. */
    Page<RouteSummaryView> searchArchivedRoutes(Pageable pageable);

    /** Retrieves a single route by ID. */
    RouteView getRoute(Long id);
}
