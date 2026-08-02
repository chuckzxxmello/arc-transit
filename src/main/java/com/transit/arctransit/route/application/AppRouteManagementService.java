package com.transit.arctransit.route.application;

import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;
import com.transit.arctransit.route.*;
import com.transit.arctransit.route.domain.Route;
import com.transit.arctransit.route.domain.RouteRepository;
import com.transit.arctransit.route.domain.RouteStatus;
import com.transit.arctransit.route.domain.RouteStop;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Internal implementation of RouteManagementService.
 *
 * Handles route CRUD and ordered stop management.
 * When creating or updating a route, stops are validated for:
 * - Non-blank stop names
 * - Positive sequence numbers
 * - No duplicate sequence numbers
 */
@Service
@Transactional
public class AppRouteManagementService implements RouteManagementService {

    private final RouteRepository routeRepository;

    public AppRouteManagementService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public RouteView createRoute(@Valid CreateRouteCommand command) {
        String normalizedRouteCode = command.routeCode().trim().toUpperCase();

        if (routeRepository.findByRouteCode(normalizedRouteCode).isPresent()) {
            throw new BusinessConflictException("Route code is already in use: " + normalizedRouteCode);
        }

        Route route = new Route(
                normalizedRouteCode,
                command.routeName().trim(),
                command.description() != null ? command.description().trim() : null,
                command.estimatedDurationMinutes()
        );

        if (command.stops() != null) {
            for (CreateRouteCommand.StopEntry entry : command.stops()) {
                route.addStop(new RouteStop(
                        entry.stopName().trim(),
                        entry.stopSequence(),
                        entry.estimatedArrivalMinutes()
                ));
            }
        }

        routeRepository.saveAndFlush(route);
        return toView(route);
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public RouteView updateRoute(@Valid UpdateRouteCommand command) {
        Route route = routeRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + command.id()));

        String normalizedRouteCode = command.routeCode().trim().toUpperCase();

        routeRepository.findByRouteCode(normalizedRouteCode)
                .filter(existing -> !existing.getId().equals(route.getId()))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("Route code is already in use: " + normalizedRouteCode);
                });

        route.updateDetails(
                normalizedRouteCode,
                command.routeName().trim(),
                command.description() != null ? command.description().trim() : null,
                command.estimatedDurationMinutes()
        );

        if (command.stops() != null) {
            List<RouteStop> newStops = command.stops().stream()
                    .map(entry -> new RouteStop(
                            entry.stopName().trim(),
                            entry.stopSequence(),
                            entry.estimatedArrivalMinutes()
                    ))
                    .toList();
            route.replaceStops(newStops);
        }

        return toView(route);
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public RouteView changeRouteStatus(Long id, String newStatus) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + id));

        try {
            RouteStatus status = RouteStatus.valueOf(newStatus);
            route.changeRouteStatus(status);
        } catch (IllegalArgumentException e) {
            throw new CommandValidationException("Invalid route status: " + newStatus);
        }

        return toView(route);
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void archiveRoute(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + id));
        route.archive();
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void unarchiveRoute(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + id));
        route.unarchive();
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void hardDeleteRoute(Long id) {
        try {
            routeRepository.deleteById(id);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new BusinessConflictException("Cannot permanently delete route " + id + " because it is referenced by other records.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public Page<RouteSummaryView> searchRoutes(RouteQuery query, Pageable pageable) {
        return routeRepository.findByArchivedAtIsNull(pageable)
                .map(route -> new RouteSummaryView(
                        route.getId(),
                        route.getRouteCode(),
                        route.getRouteName(),
                        route.getStops().size(),
                        route.getEstimatedDurationMinutes(),
                        route.getRouteStatus().name()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public Page<RouteSummaryView> searchArchivedRoutes(Pageable pageable) {
        return routeRepository.findByArchivedAtIsNotNull(pageable)
                .map(route -> new RouteSummaryView(
                        route.getId(),
                        route.getRouteCode(),
                        route.getRouteName(),
                        route.getStops().size(),
                        route.getEstimatedDurationMinutes(),
                        route.getRouteStatus().name()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public RouteView getRoute(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + id));
        return toView(route);
    }

    private RouteView toView(Route route) {
        List<RouteStopView> stopViews = route.getStops().stream()
                .map(stop -> new RouteStopView(
                        stop.getStopName(),
                        stop.getStopSequence(),
                        stop.getEstimatedArrivalMinutes()
                ))
                .toList();

        return new RouteView(
                route.getId(),
                route.getRouteCode(),
                route.getRouteName(),
                route.getDescription(),
                route.getRouteStatus().name(),
                route.getEstimatedDurationMinutes(),
                stopViews
        );
    }
}
