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
import com.transit.arctransit.route.domain.RouteVersion;
import com.transit.arctransit.route.domain.RouteVersionRepository;
import com.transit.arctransit.route.RouteVersionView;
import com.transit.arctransit.route.RouteScheduleView;
import com.transit.arctransit.route.domain.RouteScheduleRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDate;
import java.time.ZoneId;

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
@Validated
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
    private final RouteVersionRepository versionRepository;
    private final RouteScheduleRepository scheduleRepository;

    @Value("${arc.service-zone:Asia/Manila}")
    private String serviceZone = "Asia/Manila";

    // constructor

    public AppRouteManagementService(
            RouteRepository routeRepository,
            AuditRecordingService auditService,
            RouteVersionRepository versionRepository,
            RouteScheduleRepository scheduleRepository) {

        this.routeRepository = routeRepository;
        this.auditService = auditService;
        this.versionRepository = versionRepository;
        this.scheduleRepository = scheduleRepository;
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

        versionRepository.save(new RouteVersion(saved,
                command.effectiveFrom() == null ? serviceDate() : command.effectiveFrom(), null));

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
                routeRepository.findByIdForUpdate(
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

        if (route.isArchived()) {
            throw new CommandValidationException("Cannot publish a version for an archived route.");
        }

        LocalDate effectiveFrom = command.effectiveFrom() == null
                ? serviceDate() : command.effectiveFrom();
        if (effectiveFrom.isBefore(serviceDate())
                || (command.effectiveTo() != null && !command.effectiveTo().isAfter(effectiveFrom))) {
            throw new CommandValidationException("Route version dates must form a positive interval from today or later.");
        }

        RouteVersion previous = versionRepository.findFirstByRouteIdOrderByEffectiveFromDesc(route.getId())
                .orElseThrow(() -> new CommandValidationException("Route has no baseline version."));
        if (!effectiveFrom.isAfter(previous.getEffectiveFrom())) {
            throw new BusinessConflictException("New route version must begin after the latest version's start date.");
        }
        if (previous.getEffectiveTo() == null || effectiveFrom.isBefore(previous.getEffectiveTo())) {
            if (versionRepository.hasAssignmentsOnOrAfter(previous.getId(), effectiveFrom)) {
                throw new BusinessConflictException("Existing trips reference this route version on or after the new date.");
            }
            previous.closeAt(effectiveFrom);
            versionRepository.flush();
        }

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

        route.replaceStops(List.of());
        routeRepository.flush();
        route.replaceStops(newStops);
        versionRepository.save(new RouteVersion(route, effectiveFrom, command.effectiveTo()));

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

        auditService.recordAction("ROUTE_DELETED", "Route", id,
                "Permanently deleted route " + route.getRouteCode() + ".");

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

    @Override
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public RouteView lockRouteForAssignment(Long id) {
        Route route = routeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + id));
        return toView(route);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public RouteVersionView getEffectiveVersion(Long routeId, LocalDate date) {
        if (date == null) {
            throw new CommandValidationException("Service date is required.");
        }
        return versionRepository.findEffective(routeId, date).map(this::toVersionView)
                .orElseThrow(() -> new CommandValidationException(
                        "ROUTE_VERSION_NOT_EFFECTIVE: No route version applies on " + date));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public RouteVersionView getVersion(Long versionId) {
        return versionRepository.findById(versionId).map(this::toVersionView)
                .orElseThrow(() -> new ResourceNotFoundException("Route version not found: " + versionId));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public List<RouteVersionView> getVersions(Long routeId) {
        return versionRepository.findByRouteIdOrderByEffectiveFromDesc(routeId).stream()
                .map(this::toVersionView).toList();
    }

    private RouteVersionView toVersionView(RouteVersion version) {
        return new RouteVersionView(version.getId(), version.getRouteId(), version.getRouteCode(),
                version.getRouteName(), version.getDescription(), version.getEstimatedDurationMinutes(),
                version.getEffectiveFrom(), version.getEffectiveTo(), version.isLegacySnapshot(),
                version.getStops().stream().map(stop -> new RouteStopView(stop.getStopName(),
                        stop.getStopSequence(), stop.getEstimatedArrivalMinutes())).toList());
    }

    @Override
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public RouteScheduleView createSchedule(com.transit.arctransit.route.CreateRouteScheduleCommand command) {
        if (command == null || command.routeId() == null || command.name() == null
                || command.name().isBlank() || command.name().trim().length() > 100
                || command.plannedDeparture() == null || command.effectiveFrom() == null
                || (command.effectiveTo() != null && !command.effectiveTo().isAfter(command.effectiveFrom()))) {
            throw new CommandValidationException("Schedule requires a name, departure and positive effective date interval.");
        }
        Route route = routeRepository.findByIdForUpdate(command.routeId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + command.routeId()));
        if (route.isArchived()) {
            throw new CommandValidationException("Cannot configure a schedule for an archived route.");
        }
        String name = command.name().trim();
        if (scheduleRepository.existsByRouteIdAndNameAndEffectiveFrom(route.getId(), name, command.effectiveFrom())) {
            throw new BusinessConflictException("Schedule name already exists for this route and start date.");
        }
        var schedule = scheduleRepository.save(new com.transit.arctransit.route.domain.RouteSchedule(
                route.getId(), name, command.plannedDeparture(), command.effectiveFrom(), command.effectiveTo()));
        auditService.recordAction("ROUTE_SCHEDULE_CREATED", "RouteSchedule", schedule.getId(),
                "Created configured schedule for route " + route.getRouteCode());
        return toScheduleView(schedule);
    }

    @Override
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public RouteScheduleView changeScheduleActive(Long scheduleId, boolean active) {
        var schedule = findSchedule(scheduleId);
        routeRepository.findByIdForUpdate(schedule.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + schedule.getRouteId()));
        schedule.changeActive(active);
        auditService.recordAction(active ? "ROUTE_SCHEDULE_ACTIVATED" : "ROUTE_SCHEDULE_DEACTIVATED",
                "RouteSchedule", schedule.getId(), "Changed configured schedule availability");
        return toScheduleView(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public RouteScheduleView getSchedule(Long id) {
        return toScheduleView(findSchedule(id));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS_STAFF')")
    public List<RouteScheduleView> getSchedules(Long routeId) {
        return scheduleRepository.findByRouteIdOrderByPlannedDepartureAscNameAsc(routeId).stream()
                .map(this::toScheduleView).toList();
    }

    private com.transit.arctransit.route.domain.RouteSchedule findSchedule(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route schedule not found: " + id));
    }

    private RouteScheduleView toScheduleView(com.transit.arctransit.route.domain.RouteSchedule schedule) {
        return new RouteScheduleView(schedule.getId(), schedule.getRouteId(), schedule.getName(),
                schedule.getPlannedDeparture(), schedule.getEffectiveFrom(), schedule.getEffectiveTo(),
                schedule.isActive());
    }

    private LocalDate serviceDate() {
        return LocalDate.now(ZoneId.of(serviceZone));
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
                stopViews,
                route.getArchivedAt()
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
