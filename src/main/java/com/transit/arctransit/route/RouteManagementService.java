package com.transit.arctransit.route;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// public application contract for Route Management
public interface RouteManagementService {

    RouteScheduleView createSchedule(@NotNull @Valid CreateRouteScheduleCommand command);

    RouteScheduleView changeScheduleActive(@NotNull @Positive Long scheduleId, boolean active);

    RouteScheduleView getSchedule(@NotNull @Positive Long id);

    java.util.List<RouteScheduleView> getSchedules(@NotNull @Positive Long routeId);

    RouteView lockRouteForAssignment(@NotNull @Positive Long id);

    RouteVersionView getEffectiveVersion(@NotNull @Positive Long routeId, java.time.LocalDate serviceDate);

    RouteVersionView getVersion(@NotNull @Positive Long versionId);

    java.util.List<RouteVersionView> getVersions(@NotNull @Positive Long routeId);


    RouteView createRoute(
            @NotNull @Valid CreateRouteCommand command
    );

    RouteView updateRoute(
            @NotNull @Valid UpdateRouteCommand command
    );

    RouteView changeRouteStatus(
            @NotNull @Positive Long id,
            @NotBlank @Size(max = 50) @SafeText String newStatus
    );

    // archive route
    void archiveRoute(@NotNull @Positive Long id);

    // restore route
    void unarchiveRoute(@NotNull @Positive Long id);

    // permanently delete route
    void hardDeleteRoute(@NotNull @Positive Long id);

    // search active routes
    Page<RouteSummaryView> searchRoutes(
            @Valid RouteQuery query,
            Pageable pageable
    );

    // search archived routes
    Page<RouteSummaryView> searchArchivedRoutes(
            Pageable pageable
    );

    // retrieve route by ID
    RouteView getRoute(@NotNull @Positive Long id);
}
