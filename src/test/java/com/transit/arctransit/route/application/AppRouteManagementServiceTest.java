package com.transit.arctransit.route.application;

// route module

import com.transit.arctransit.route.CreateRouteCommand;
import com.transit.arctransit.route.RouteSummaryView;
import com.transit.arctransit.route.RouteView;

// route domain

import com.transit.arctransit.route.domain.Route;
import com.transit.arctransit.route.domain.RouteRepository;
import com.transit.arctransit.route.domain.RouteVersion;
import com.transit.arctransit.route.domain.RouteVersionRepository;

// audit

import com.transit.arctransit.audit.AuditRecordingService;

// common exceptions

import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;

// junit

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// mockito

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// spring data

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

// java

import java.util.List;
import java.util.Optional;

// assertions

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// mockito

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AppRouteManagementService.
 *
 * Tests route application logic without requiring
 * a real database.
 */
@ExtendWith(MockitoExtension.class)
class AppRouteManagementServiceTest {

    // dependencies

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private AuditRecordingService auditService;

    @Mock
    private RouteVersionRepository versionRepository;

    @Mock
    private com.transit.arctransit.route.domain.RouteScheduleRepository scheduleRepository;

    @InjectMocks
    private AppRouteManagementService service;

    // test fixture

    private Route existingRoute;

    @BeforeEach
    void setUp() {

        existingRoute =
                new Route(
                        "032GX",
                        "Mendez-PITX-Ayala One",
                        "Main route",
                        90
                );

        // assign deterministic ID for unit testing

        setEntityId(
                existingRoute,
                1L
        );
    }

    // create route

    @Test
    void createRoute_success() {

        when(routeRepository.findByRouteCode("ROUTE-A"))
                .thenReturn(Optional.empty());

        when(routeRepository.save(any(Route.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        CreateRouteCommand command =
                new CreateRouteCommand(
                        "route-a",
                        "Test Route A",
                        "Description",
                        60,
                        List.of(
                                new CreateRouteCommand.StopEntry(
                                        "Stop 1",
                                        1,
                                        10
                                ),
                                new CreateRouteCommand.StopEntry(
                                        "Stop 2",
                                        2,
                                        25
                                )
                        )
                );

        RouteView result =
                service.createRoute(command);

        assertNotNull(result);

        assertEquals(
                "ROUTE-A",
                result.routeCode()
        );

        assertEquals(
                "Test Route A",
                result.routeName()
        );

        assertEquals(
                2,
                result.stops().size()
        );

        verify(routeRepository)
                .save(any(Route.class));
    }

    @Test
    void createRoute_duplicateRouteCode_throwsConflict() {

        when(routeRepository.findByRouteCode("032GX"))
                .thenReturn(
                        Optional.of(existingRoute)
                );

        CreateRouteCommand command =
                new CreateRouteCommand(
                        "032GX",
                        "Duplicate Route",
                        null,
                        30,
                        null
                );

        assertThrows(
                BusinessConflictException.class,
                () -> service.createRoute(command)
        );
    }

    @Test
    void createRoute_withNoStops_success() {

        when(routeRepository.findByRouteCode("ROUTE-B"))
                .thenReturn(Optional.empty());

        when(routeRepository.save(any(Route.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        CreateRouteCommand command =
                new CreateRouteCommand(
                        "route-b",
                        "Route B",
                        null,
                        45,
                        null
                );

        RouteView result =
                service.createRoute(command);

        assertNotNull(result);

        assertEquals(
                "ROUTE-B",
                result.routeCode()
        );

        assertEquals(
                0,
                result.stops().size()
        );
    }

    // route status

    @Test
    void changeRouteStatus_success() {

        when(routeRepository.findById(1L))
                .thenReturn(
                        Optional.of(existingRoute)
                );

        RouteView result =
                service.changeRouteStatus(
                        1L,
                        "INACTIVE"
                );

        assertEquals(
                "INACTIVE",
                result.routeStatus()
        );
    }

    // archive

    @Test
    void archiveRoute_success() {

        when(routeRepository.findById(1L))
                .thenReturn(
                        Optional.of(existingRoute)
                );

        service.archiveRoute(1L);

        assertNotNull(
                existingRoute.getArchivedAt()
        );
    }

    // get route

    @Test
    void getRoute_notFound_throwsNotFound() {

        when(routeRepository.findById(99L))
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getRoute(99L)
        );
    }

    // search

    @Test
    void searchRoutes_returnsPagedResults() {

        Page<Route> page =
                new PageImpl<>(
                        List.of(existingRoute)
                );

        /*
         * The cleaned application service uses searchActive()
         * instead of findByArchivedAtIsNull().
         *
         * null search filter means no text filter.
         * null status means no status filter.
         */
        when(routeRepository.searchActive(
                null,
                null,
                PageRequest.of(0, 10)
        )).thenReturn(page);

        Page<RouteSummaryView> result =
                service.searchRoutes(
                        null,
                        PageRequest.of(0, 10)
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(
                "032GX",
                result.getContent()
                        .get(0)
                        .routeCode()
        );
    }

    @Test
    void publishNewVersion_preservesPreviousDefinitionAndStops() {
        var today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Manila"));
        existingRoute.addStop(new com.transit.arctransit.route.domain.RouteStop("Original stop", 1, 0));
        var previous = new RouteVersion(existingRoute, today.minusDays(7), null);
        setVersionId(previous, 10L);
        when(routeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingRoute));
        when(routeRepository.findByRouteCode("032GX")).thenReturn(Optional.of(existingRoute));
        when(versionRepository.findFirstByRouteIdOrderByEffectiveFromDesc(1L))
                .thenReturn(Optional.of(previous));

        service.updateRoute(new com.transit.arctransit.route.UpdateRouteCommand(1L,
                "032GX", "Later route definition", "Later configuration", 100,
                List.of(new CreateRouteCommand.StopEntry("New stop", 1, 0)), today, null));

        assertEquals(today, previous.getEffectiveTo());
        assertEquals("Mendez-PITX-Ayala One", previous.getRouteName());
        assertEquals("Original stop", previous.getStops().getFirst().getStopName());
        assertEquals("Later route definition", existingRoute.getRouteName());
        var version = org.mockito.ArgumentCaptor.forClass(RouteVersion.class);
        verify(versionRepository).save(version.capture());
        assertEquals("New stop", version.getValue().getStops().getFirst().getStopName());
        verify(routeRepository).flush();
    }

    @Test
    void publishNewVersion_rejectsInvalidatingAlreadyPlannedTrip() {
        var today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Manila"));
        var previous = new RouteVersion(existingRoute, today.minusDays(7), null);
        setVersionId(previous, 10L);
        when(routeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingRoute));
        when(routeRepository.findByRouteCode("032GX")).thenReturn(Optional.of(existingRoute));
        when(versionRepository.findFirstByRouteIdOrderByEffectiveFromDesc(1L))
                .thenReturn(Optional.of(previous));
        when(versionRepository.hasAssignmentsOnOrAfter(10L, today)).thenReturn(true);

        assertThrows(BusinessConflictException.class, () -> service.updateRoute(
                new com.transit.arctransit.route.UpdateRouteCommand(1L, "032GX",
                        "Later definition", null, 60, List.of(), today, null)));
        assertEquals("Mendez-PITX-Ayala One", existingRoute.getRouteName());
        org.junit.jupiter.api.Assertions.assertNull(previous.getEffectiveTo());
        org.mockito.Mockito.verify(versionRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void effectiveVersion_respectsExclusiveEndBoundary() {
        var start = java.time.LocalDate.of(2026, 11, 1);
        var end = start.plusDays(10);
        var version = new RouteVersion(existingRoute, start, end);
        org.junit.jupiter.api.Assertions.assertTrue(version.isEffectiveOn(start));
        org.junit.jupiter.api.Assertions.assertTrue(version.isEffectiveOn(end.minusDays(1)));
        org.junit.jupiter.api.Assertions.assertFalse(version.isEffectiveOn(end));
        org.junit.jupiter.api.Assertions.assertFalse(version.isEffectiveOn(start.minusDays(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new RouteVersion(existingRoute, start, start));
    }

    @Test
    void publicationRejectsSameStartDateWithoutChangingDefinition() {
        var today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Manila"));
        var previous = new RouteVersion(existingRoute, today, null);
        when(routeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingRoute));
        when(routeRepository.findByRouteCode("032GX")).thenReturn(Optional.of(existingRoute));
        when(versionRepository.findFirstByRouteIdOrderByEffectiveFromDesc(1L))
                .thenReturn(Optional.of(previous));
        assertThrows(BusinessConflictException.class, () -> service.updateRoute(
                new com.transit.arctransit.route.UpdateRouteCommand(1L, "032GX",
                        "Later definition", null, 60, List.of(), today, null)));
    }

    private static void setVersionId(RouteVersion version, Long id) {
        try {
            var field = RouteVersion.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(version, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void configuredSchedule_keepsDefinitionWhenDeactivated() {
        var start = java.time.LocalDate.of(2026, 11, 1);
        var departure = java.time.LocalTime.of(8, 30);
        var schedule = new com.transit.arctransit.route.domain.RouteSchedule(
                1L, "Morning service", departure, start, start.plusMonths(1));
        when(scheduleRepository.findById(30L)).thenReturn(Optional.of(schedule));
        when(routeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingRoute));

        var result = service.changeScheduleActive(30L, false);

        org.junit.jupiter.api.Assertions.assertFalse(result.active());
        assertEquals(departure, result.plannedDeparture());
        assertEquals(start, result.effectiveFrom());
        assertEquals(start.plusMonths(1), result.effectiveTo());
    }

    @Test
    void configuredSchedule_rejectsNonPositiveEffectiveInterval() {
        var start = java.time.LocalDate.of(2026, 11, 1);
        assertThrows(com.transit.arctransit.common.exception.CommandValidationException.class,
                () -> service.createSchedule(new com.transit.arctransit.route.CreateRouteScheduleCommand(
                        1L, "Morning", java.time.LocalTime.of(8, 30), start, start)));
    }

    @Test
    void configuredSchedule_creationProducesReusableTemplate() {
        var start = java.time.LocalDate.of(2026, 11, 1);
        var departure = java.time.LocalTime.of(8, 30);
        when(routeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existingRoute));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.createSchedule(new com.transit.arctransit.route.CreateRouteScheduleCommand(
                1L, "Morning", departure, start, null));
        org.junit.jupiter.api.Assertions.assertTrue(result.active());
        assertEquals(1L, result.routeId());
        assertEquals(departure, result.plannedDeparture());
        org.junit.jupiter.api.Assertions.assertTrue(result.isEffectiveOn(start));
    }

    // test utility

    private static void setEntityId(
            Route route,
            Long id) {

        try {

            var idField =
                    Route.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(route, id);

        } catch (ReflectiveOperationException exception) {

            throw new IllegalStateException(
                    "Unable to initialize test route ID.",
                    exception
            );
        }
    }
}
