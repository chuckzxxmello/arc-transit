package com.transit.arctransit.route;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// public application contract for Route Management
public interface RouteManagementService {

    RouteView createRoute(
            @Valid CreateRouteCommand command
    );

    RouteView updateRoute(
            @Valid UpdateRouteCommand command
    );

    RouteView changeRouteStatus(
            Long id,
            String newStatus
    );

    // archive route
    void archiveRoute(Long id);

    // restore route
    void unarchiveRoute(Long id);

    // permanently delete route
    void hardDeleteRoute(Long id);

    // search active routes
    Page<RouteSummaryView> searchRoutes(
            RouteQuery query,
            Pageable pageable
    );

    // search archived routes
    Page<RouteSummaryView> searchArchivedRoutes(
            Pageable pageable
    );

    // retrieve route by ID
    RouteView getRoute(Long id);
}