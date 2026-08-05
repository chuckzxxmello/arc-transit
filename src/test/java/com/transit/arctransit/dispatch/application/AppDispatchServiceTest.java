package com.transit.arctransit.dispatch.application;

import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.dispatch.*;
import com.transit.arctransit.dispatch.domain.DispatchAssignment;
import com.transit.arctransit.dispatch.domain.DispatchAssignmentRepository;
import com.transit.arctransit.dispatch.domain.DispatchStatus;
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.driver.DriverView;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitView;
import com.transit.arctransit.route.RouteManagementService;
import com.transit.arctransit.route.RouteView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppDispatchService.
 * Tests the 5-step cross-module validation and state machine transitions.
 *
 * Source: https://site.mockito.org/
 * Source: https://junit.org/junit5/docs/current/user-guide/
 */
@ExtendWith(MockitoExtension.class)
class AppDispatchServiceTest {

    @Mock
    private DispatchAssignmentRepository assignmentRepository;
    @Mock
    private FleetManagementService fleetService;
    @Mock
    private DriverManagementService driverService;
    @Mock
    private RouteManagementService routeService;
    @Mock
    private com.transit.arctransit.audit.AuditRecordingService auditService;

    @InjectMocks
    private AppDispatchService service;

    private FleetUnitView activeFleetUnit;
    private FleetUnitView inactiveFleetUnit;
    private DriverView activeDriver;
    private DriverView expiredLicenseDriver;
    private RouteView activeRoute;
    private RouteView inactiveRoute;

    @BeforeEach
    void setUp() {
        activeFleetUnit = new FleetUnitView(1L, "BUS-001", "ABC-1234", "BUS", (short) 50, "ACTIVE");
        inactiveFleetUnit = new FleetUnitView(2L, "BUS-002", "XYZ-5678", "BUS", (short) 40, "INACTIVE");

        activeDriver = new DriverView(1L, "EMP-001", "Juan", "Dela Cruz",
                "LIC-001", "PROFESSIONAL", LocalDate.now().plusYears(1).toString(),
                "09171234567", "ACTIVE", false);
        expiredLicenseDriver = new DriverView(2L, "EMP-002", "Pedro", "Santos",
                "LIC-002", "PROFESSIONAL", LocalDate.now().minusDays(1).toString(),
                "09181234567", "ACTIVE", true);

        activeRoute = new RouteView(1L, "032GX", "Mendez-PITX", "Desc", "ACTIVE", 90, List.of());
        inactiveRoute = new RouteView(2L, "033GX", "Imus Route", "Desc", "INACTIVE", 60, List.of());
    }

    private CreateDispatchCommand validCommand() {
        return new CreateDispatchCommand(1L, 1L, 1L,
                LocalDate.now(), Instant.now(), null, "Test dispatch");
    }

    @Test
    void createAssignment_success() {
        when(fleetService.getUnit(1L)).thenReturn(activeFleetUnit);
        when(driverService.getDriver(1L)).thenReturn(activeDriver);
        when(routeService.getRoute(1L)).thenReturn(activeRoute);
        when(assignmentRepository.findByFleetUnitIdAndDispatchDateAndDispatchStatusNot(
                eq(1L), any(), eq(DispatchStatus.CANCELLED))).thenReturn(Collections.emptyList());
        when(assignmentRepository.findByDriverIdAndDispatchDateAndDispatchStatusNot(
                eq(1L), any(), eq(DispatchStatus.CANCELLED))).thenReturn(Collections.emptyList());
        when(assignmentRepository.save(any(DispatchAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DispatchAssignmentView result = service.createAssignment(validCommand());

        assertNotNull(result);
        assertEquals("SCHEDULED", result.dispatchStatus());
        assertEquals("BUS-001", result.fleetUnitNumber());
        verify(assignmentRepository).save(any(DispatchAssignment.class));
    }

    @Test
    void createAssignment_inactiveFleetUnit_throwsValidation() {
        when(fleetService.getUnit(2L)).thenReturn(inactiveFleetUnit);

        CreateDispatchCommand cmd = new CreateDispatchCommand(2L, 1L, 1L,
                LocalDate.now(), Instant.now(), null, null);
        assertThrows(CommandValidationException.class, () -> service.createAssignment(cmd));
    }

    @Test
    void createAssignment_expiredDriverLicense_throwsValidation() {
        when(fleetService.getUnit(1L)).thenReturn(activeFleetUnit);
        when(driverService.getDriver(2L)).thenReturn(expiredLicenseDriver);

        CreateDispatchCommand cmd = new CreateDispatchCommand(1L, 2L, 1L,
                LocalDate.now(), Instant.now(), null, null);
        assertThrows(CommandValidationException.class, () -> service.createAssignment(cmd));
    }

    @Test
    void createAssignment_inactiveRoute_throwsValidation() {
        when(fleetService.getUnit(1L)).thenReturn(activeFleetUnit);
        when(driverService.getDriver(1L)).thenReturn(activeDriver);
        when(routeService.getRoute(2L)).thenReturn(inactiveRoute);

        CreateDispatchCommand cmd = new CreateDispatchCommand(1L, 1L, 2L,
                LocalDate.now(), Instant.now(), null, null);
        assertThrows(CommandValidationException.class, () -> service.createAssignment(cmd));
    }

    @Test
    void createAssignment_busOverlap_throwsConflict() {
        when(fleetService.getUnit(1L)).thenReturn(activeFleetUnit);
        when(driverService.getDriver(1L)).thenReturn(activeDriver);
        when(routeService.getRoute(1L)).thenReturn(activeRoute);

        DispatchAssignment conflicting = new DispatchAssignment(
                1L, 3L, 1L, LocalDate.now(), Instant.now(), null, null);
        when(assignmentRepository.findByFleetUnitIdAndDispatchDateAndDispatchStatusNot(
                eq(1L), any(), eq(DispatchStatus.CANCELLED)))
                .thenReturn(List.of(conflicting));

        assertThrows(BusinessConflictException.class, () -> service.createAssignment(validCommand()));
    }

    @Test
    void createAssignment_driverOverlap_throwsConflict() {
        when(fleetService.getUnit(1L)).thenReturn(activeFleetUnit);
        when(driverService.getDriver(1L)).thenReturn(activeDriver);
        when(routeService.getRoute(1L)).thenReturn(activeRoute);
        when(assignmentRepository.findByFleetUnitIdAndDispatchDateAndDispatchStatusNot(
                eq(1L), any(), eq(DispatchStatus.CANCELLED))).thenReturn(Collections.emptyList());

        DispatchAssignment conflicting = new DispatchAssignment(
                3L, 1L, 1L, LocalDate.now(), Instant.now(), null, null);
        when(assignmentRepository.findByDriverIdAndDispatchDateAndDispatchStatusNot(
                eq(1L), any(), eq(DispatchStatus.CANCELLED)))
                .thenReturn(List.of(conflicting));

        assertThrows(BusinessConflictException.class, () -> service.createAssignment(validCommand()));
    }

    @Test
    void startTrip_success() {
        DispatchAssignment assignment = new DispatchAssignment(
                1L, 1L, 1L, LocalDate.now(), Instant.now(), null, null);
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(fleetService.getUnit(1L)).thenReturn(activeFleetUnit);
        when(driverService.getDriver(1L)).thenReturn(activeDriver);
        when(routeService.getRoute(1L)).thenReturn(activeRoute);

        DispatchAssignmentView result = service.startTrip(1L);
        assertEquals("IN_PROGRESS", result.dispatchStatus());
    }

    @Test
    void completeTrip_afterStart_success() {
        DispatchAssignment assignment = new DispatchAssignment(
                1L, 1L, 1L, LocalDate.now(), Instant.now(), null, null);
        assignment.startTrip();
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(fleetService.getUnit(1L)).thenReturn(activeFleetUnit);
        when(driverService.getDriver(1L)).thenReturn(activeDriver);
        when(routeService.getRoute(1L)).thenReturn(activeRoute);

        DispatchAssignmentView result = service.completeTrip(1L);
        assertEquals("COMPLETED", result.dispatchStatus());
    }

    @Test
    void cancelAssignment_success() {
        DispatchAssignment assignment = new DispatchAssignment(
                1L, 1L, 1L, LocalDate.now(), Instant.now(), null, null);
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(fleetService.getUnit(1L)).thenReturn(activeFleetUnit);
        when(driverService.getDriver(1L)).thenReturn(activeDriver);
        when(routeService.getRoute(1L)).thenReturn(activeRoute);

        DispatchAssignmentView result = service.cancelAssignment(1L);
        assertEquals("CANCELLED", result.dispatchStatus());
    }
}
