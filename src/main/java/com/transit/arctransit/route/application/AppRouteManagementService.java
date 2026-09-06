package com.transit.arctransit.route.application;

// route module

import com.transit.arctransit.route.CreateRouteCommand;
import com.transit.arctransit.route.RouteManagementService;
import com.transit.arctransit.route.RouteQuery;
import com.transit.arctransit.route.RouteStopView;
import com.transit.arctransit.route.RouteSummaryView;
import com.transit.arctransit.route.RouteView;
import com.transit.arctransit.route.UpdateRouteCommand;

// route domain

import com.transit.arctransit.route.domain.Route;
import com.transit.arctransit.route.domain.RouteRepository;
import com.transit.arctransit.route.domain.RouteStatus;
import com.transit.arctransit.route.domain.RouteStop;

// common and audit

import com.transit.arctransit.audit.AuditRecordingService;
import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;

// java

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// spring

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// route management service

@Service
@Transactional
public class AppRouteManagementService
        implements RouteManagementService {

    // constants

    private static final Logger log =
            LoggerFactory.getLogger(
                    AppRouteManagementService.class
            );

    // dependencies

    private final RouteRepository routeRepository;

    private final AuditRecordingService auditService;

    // constructor

    public AppRouteManagementService(
            RouteRepository routeRepository,
            AuditRecordingService auditService) {

        this.routeRepository = routeRepository;
        this.auditService = auditService;
    }

    // create route

    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public RouteView createRoute(
            CreateRouteCommand command) {

        String routeCode =
                normalizeRouteCode(
                        command.routeCode()
                );

        String routeName =
                normalizeRequiredText(
                        command.routeName(),
                        "Route name"
                );

        String description =
                normalizeOptionalText(
                        command.description()
                );

        validateDuration(
                command.estimatedDurationMinutes()
        );

        validateStops(
                command.stops()
        );

        // check duplicate route code

        if (routeRepository
                .findByRouteCode(routeCode)
                .isPresent()) {

            throw new BusinessConflictException(
                    "Route code is already in use: "
                            + routeCode
            );
        }

        Route route =
                new Route(
                        routeCode,
                        routeName,
                        description,
                        command.estimatedDurationMinutes()
                );

        // add route stops

        for (CreateRouteCommand.StopEntry entry :
                command.stops()) {

            route.addStop(
                    new RouteStop(
                            normalizeRequiredText(
                                    entry.stopName(),
                                    "Stop name"
                            ),
                            entry.stopSequence(),
                            entry.estimatedArrivalMinutes()
                    )
            );
        }

        Route saved =
                routeRepository.save(route);

        auditService.recordAction(
                "ROUTE_CREATED",
                "Route",
                saved.getId(),
                "Created route "
                        + saved.getRouteCode()
        );

        log.info(
                "Created route '{}'.",
                saved.getRouteCode()
        );

        return toView(saved);
    }

    // update route

    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public RouteView updateRoute(
            UpdateRouteCommand command) {

        Route route =
                routeRepository.findById(
                        command.id()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Route not found: "
                                        + command.id()
                        )
                );

        String routeCode =
                normalizeRouteCode(
                        command.routeCode()
                );

        String routeName =
                normalizeRequiredText(
                        command.routeName(),
                        "Route name"
                );

        String description =
                normalizeOptionalText(
                        command.description()
                );

        validateDuration(
                command.estimatedDurationMinutes()
        );

        validateStops(
                command.stops()
        );

        // check duplicate route code

        routeRepository
                .findByRouteCode(routeCode)
                .filter(existing ->
                        !existing.getId().equals(
                                route.getId()
                        )
                )
                .ifPresent(existing -> {
                    throw new BusinessConflictException(
                            "Route code is already in use: "
                                    + routeCode
                    );
                });

        route.updateDetails(
                routeCode,
                routeName,
                description,
                command.estimatedDurationMinutes()
        );

        // replace route stops

        List<RouteStop> newStops =
                command.stops()
                        .stream()
                        .map(entry ->
                                new RouteStop(
                                        normalizeRequiredText(
                                                entry.stopName(),
                                                "Stop name"
                                        ),
                                        entry.stopSequence(),
                                        entry.estimatedArrivalMinutes()
                                )
                        )
                        .toList();

        route.replaceStops(newStops);

        auditService.recordAction(
                "ROUTE_UPDATED",
                "Route",
                route.getId(),
                "Updated route "
                        + route.getRouteCode()
        );

        log.info(
                "Updated route '{}'.",
                route.getRouteCode()
        );

        return toView(route);
    }

    // change route status

    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public RouteView changeRouteStatus(
            Long id,
            String newStatus) {

        Route route =
                routeRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Route not found: "
                                                + id
                                )
                        );

        RouteStatus status =
                parseRouteStatus(newStatus);

        route.changeRouteStatus(status);

        auditService.recordAction(
                "ROUTE_STATUS_CHANGED",
                "Route",
                route.getId(),
                "Status changed to "
                        + status.name()
                        + " for route "
                        + route.getRouteCode()
        );

        log.info(
                "Changed route '{}' status to '{}'.",
                route.getRouteCode(),
                status
        );

        return toView(route);
    }

    // archive route

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void archiveRoute(Long id) {

        Route route =
                routeRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Route not found: "
                                                + id
                                )
                        );

        route.archive();

        auditService.recordAction(
                "ROUTE_ARCHIVED",
                "Route",
                id,
                "Archived route "
                        + route.getRouteCode()
        );

        log.info(
                "Archived route '{}'.",
                route.getRouteCode()
        );
    }

    // restore route

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void unarchiveRoute(Long id) {

        Route route =
                routeRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Route not found: "
                                                + id
                                )
                        );

        route.unarchive();

        auditService.recordAction(
                "ROUTE_UNARCHIVED",
                "Route",
                id,
                "Unarchived route "
                        + route.getRouteCode()
        );

        log.info(
                "Unarchived route '{}'.",
                route.getRouteCode()
        );
    }

    // permanently delete route

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void hardDeleteRoute(Long id) {

        Route route =
                routeRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Route not found: "
                                                + id
                                )
                        );

        try {

            // flush immediately so database constraint
            // violations are raised inside this try block

            routeRepository.delete(route);
            routeRepository.flush();

        } catch (DataIntegrityViolationException exception) {

            throw new BusinessConflictException(
                    "Cannot permanently delete route "
                            + id
                            + " because it is referenced "
                            + "by other records."
            );
        }

        log.info(
                "Permanently deleted route '{}'.",
                route.getRouteCode()
        );
    }

    // search active routes

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public Page<RouteSummaryView> searchRoutes(
            RouteQuery query,
            Pageable pageable) {

        String searchFilter =
                query == null
                        ? null
                        : normalizeOptionalText(
                                query.routeOrNameFilter()
                        );

        RouteStatus status =
                query == null
                        ? null
                        : parseOptionalRouteStatus(
                                query.statusFilter()
                        );

        return routeRepository
                .searchActive(
                        searchFilter,
                        status,
                        pageable
                )
                .map(this::toSummaryView);
    }

    // search archived routes

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public Page<RouteSummaryView> searchArchivedRoutes(
            Pageable pageable) {

        return routeRepository
                .findByArchivedAtIsNotNull(pageable)
                .map(this::toSummaryView);
    }

    // get route

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public RouteView getRoute(Long id) {

        Route route =
                routeRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Route not found: "
                                                + id
                                )
                        );

        return toView(route);
    }

    // map route to detailed view

    private RouteView toView(
            Route route) {

        List<RouteStopView> stopViews =
                route.getStops()
                        .stream()
                        .map(stop ->
                                new RouteStopView(
                                        stop.getStopName(),
                                        stop.getStopSequence(),
                                        stop.getEstimatedArrivalMinutes()
                                )
                        )
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

    // map route to summary view

    private RouteSummaryView toSummaryView(
            Route route) {

        return new RouteSummaryView(
                route.getId(),
                route.getRouteCode(),
                route.getRouteName(),
                route.getStops().size(),
                route.getEstimatedDurationMinutes(),
                route.getRouteStatus().name()
        );
    }

    // validate route stops

    private static void validateStops(
            List<CreateRouteCommand.StopEntry> stops) {

        if (stops == null) {
            return;
        }

        Set<Integer> sequences =
                new HashSet<>();

        for (CreateRouteCommand.StopEntry entry :
                stops) {

            if (entry == null) {

                throw new CommandValidationException(
                        "Route stop cannot be null."
                );
            }

            normalizeRequiredText(
                    entry.stopName(),
                    "Stop name"
            );

            if (entry.stopSequence() <= 0) {

                throw new CommandValidationException(
                        "Stop sequence must be greater than zero."
                );
            }

            if (!sequences.add(
                    entry.stopSequence()
            )) {

                throw new CommandValidationException(
                        "Duplicate stop sequence: "
                                + entry.stopSequence()
                );
            }

            if (entry.estimatedArrivalMinutes() != null
                    && entry.estimatedArrivalMinutes() < 0) {

                throw new CommandValidationException(
                        "Estimated arrival minutes cannot be negative."
                );
            }
        }
    }

    // validate route duration

    private static void validateDuration(
            Integer duration) {

        if (duration != null && duration <= 0) {

            throw new CommandValidationException(
                    "Estimated duration must be greater than zero."
            );
        }
    }

    // normalize route code

    private static String normalizeRouteCode(
            String value) {

        if (value == null || value.isBlank()) {

            throw new CommandValidationException(
                    "Route code cannot be blank."
            );
        }

        return value
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    // normalize required text

    private static String normalizeRequiredText(
            String value,
            String fieldName) {

        if (value == null || value.isBlank()) {

            throw new CommandValidationException(
                    fieldName + " cannot be blank."
            );
        }

        return value.trim();
    }

    // normalize optional text

    private static String normalizeOptionalText(
            String value) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isBlank()
                ? null
                : normalized;
    }

    // parse route status

    private static RouteStatus parseRouteStatus(
            String value) {

        if (value == null || value.isBlank()) {

            throw new CommandValidationException(
                    "Route status cannot be blank."
            );
        }

        try {

            return RouteStatus.valueOf(
                    value
                            .trim()
                            .toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException exception) {

            throw new CommandValidationException(
                    "Invalid route status: "
                            + value
            );
        }
    }

    // parse optional route status

    private static RouteStatus parseOptionalRouteStatus(
            String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return parseRouteStatus(value);
    }
}