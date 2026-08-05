package com.transit.arctransit.route.application;

import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;
import com.transit.arctransit.route.*;
import com.transit.arctransit.route.domain.Route;
import com.transit.arctransit.route.domain.RouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppRouteManagementService.
 *
 * Source: https://site.mockito.org/
 * Source: https://junit.org/junit5/docs/current/user-guide/
 */
@ExtendWith(MockitoExtension.class)
class AppRouteManagementServiceTest {

    @Mock
    private RouteRepository routeRepository;
    
    @Mock
    private com.transit.arctransit.audit.AuditRecordingService auditService;

    @InjectMocks
    private AppRouteManagementService service;

    private Route existingRoute;

    @BeforeEach
    void setUp() {
        existingRoute = new Route("032GX", "Mendez-PITX-Ayala One", "Main route", 90);
        try {
            var idField = Route.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(existingRoute, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createRoute_success() {
        when(routeRepository.findByRouteCode("ROUTE-A")).thenReturn(Optional.empty());
        when(routeRepository.save(any(Route.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateRouteCommand cmd = new CreateRouteCommand(
                "route-a", "Test Route A", "Description", 60,
                List.of(new CreateRouteCommand.StopEntry("Stop 1", 1, 10),
                        new CreateRouteCommand.StopEntry("Stop 2", 2, 25))
        );
        RouteView result = service.createRoute(cmd);

        assertNotNull(result);
        assertEquals("ROUTE-A", result.routeCode());
        assertEquals("Test Route A", result.routeName());
        assertEquals(2, result.stops().size());
        verify(routeRepository).save(any(Route.class));
    }

    @Test
    void createRoute_duplicateRouteCode_throwsConflict() {
        when(routeRepository.findByRouteCode("032GX")).thenReturn(Optional.of(existingRoute));

        CreateRouteCommand cmd = new CreateRouteCommand(
                "032GX", "Duplicate Route", null, 30, null
        );
        assertThrows(BusinessConflictException.class, () -> service.createRoute(cmd));
    }

    @Test
    void createRoute_withNoStops_success() {
        when(routeRepository.findByRouteCode("ROUTE-B")).thenReturn(Optional.empty());
        when(routeRepository.save(any(Route.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateRouteCommand cmd = new CreateRouteCommand(
                "route-b", "Route B", null, 45, null
        );
        RouteView result = service.createRoute(cmd);
        assertNotNull(result);
        assertEquals(0, result.stops().size());
    }

    @Test
    void changeRouteStatus_success() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(existingRoute));

        RouteView result = service.changeRouteStatus(1L, "INACTIVE");
        assertEquals("INACTIVE", result.routeStatus());
    }

    @Test
    void archiveRoute_success() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(existingRoute));
        service.archiveRoute(1L);
        assertNotNull(existingRoute.getArchivedAt());
    }

    @Test
    void getRoute_notFound_throwsNotFound() {
        when(routeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getRoute(99L));
    }

    @Test
    void searchRoutes_returnsPagedResults() {
        Page<Route> page = new PageImpl<>(List.of(existingRoute));
        when(routeRepository.findByArchivedAtIsNull(any())).thenReturn(page);

        Page<RouteSummaryView> result = service.searchRoutes(null, PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
        assertEquals("032GX", result.getContent().get(0).routeCode());
    }
}
