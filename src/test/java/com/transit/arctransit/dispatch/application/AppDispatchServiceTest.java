package com.transit.arctransit.dispatch.application;

import com.transit.arctransit.audit.AuditRecordingService;
import com.transit.arctransit.dispatch.CreateDispatchCommand;
import com.transit.arctransit.dispatch.DispatchAssignmentView;
import com.transit.arctransit.dispatch.DispatchConflictException;
import com.transit.arctransit.dispatch.DispatchQuery;
import com.transit.arctransit.dispatch.DispatchRule;
import com.transit.arctransit.dispatch.DispatchValidationException;
import com.transit.arctransit.dispatch.domain.DispatchAssignment;
import com.transit.arctransit.dispatch.domain.DispatchAssignmentRepository;
import com.transit.arctransit.dispatch.domain.DispatchEvent;
import com.transit.arctransit.dispatch.domain.DispatchEventRepository;
import com.transit.arctransit.dispatch.domain.DispatchStatus;
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.driver.DriverView;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitView;
import com.transit.arctransit.maintenance.MaintenanceService;
import com.transit.arctransit.route.RouteManagementService;
import com.transit.arctransit.route.RouteVersionView;
import com.transit.arctransit.route.RouteView;
import com.transit.arctransit.common.exception.CommandValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppDispatchServiceTest {

    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 11, 1);
    private static final Instant START = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant END = START.plusSeconds(3600);

    @Mock private DispatchAssignmentRepository assignmentRepository;
    @Mock private FleetManagementService fleetService;
    @Mock private DriverManagementService driverService;
    @Mock private RouteManagementService routeService;
    @Mock private AuditRecordingService auditService;
    @Mock private MaintenanceService maintenanceService;
    @Mock private DispatchEventRepository eventRepository;
    @InjectMocks private AppDispatchService service;

    private FleetUnitView availableFleet;
    private DriverView availableDriver;
    private RouteView activeRoute;
    private RouteVersionView routeVersion;

    @BeforeEach
    void setUp() {
        availableFleet = fleet("AVAILABLE", null);
        availableDriver = driver("AVAILABLE", SERVICE_DATE.plusYears(1), null);
        activeRoute = new RouteView(1L, "CURRENT", "Current definition", null, "ACTIVE", 60, List.of());
        routeVersion = new RouteVersionView(10L, 1L, "ORIGINAL", "Original definition", null,
                60, SERVICE_DATE.minusMonths(1), null, false, List.of());
    }

    private CreateDispatchCommand command() {
        return new CreateDispatchCommand(1L, 1L, 1L, SERVICE_DATE, START, END, "Staff plan");
    }

    private FleetUnitView fleet(String availability, Instant archivedAt) {
        return new FleetUnitView(1L, "BUS-001", "ABC-1234", "BUS", (short) 50,
                "ACTIVE", "ACTIVE", availability, archivedAt);
    }

    private DriverView driver(String availability, LocalDate licenseExpiry, Instant archivedAt) {
        return new DriverView(1L, "EMP-001", "Juan", "Cruz", "LIC-001", "PROFESSIONAL",
                licenseExpiry.toString(), null, "ACTIVE", false, availability, archivedAt);
    }

    private void stubFleet() {
        when(fleetService.lockUnitForAssignment(1L)).thenReturn(availableFleet);
    }

    private void stubDriver() {
        stubFleet();
        when(driverService.lockDriverForAssignment(1L)).thenReturn(availableDriver);
    }

    private void stubRoute() {
        stubDriver();
        when(routeService.lockRouteForAssignment(1L)).thenReturn(activeRoute);
        when(routeService.getEffectiveVersion(1L, SERVICE_DATE)).thenReturn(routeVersion);
    }

    private void stubSaveAndDisplay() {
        when(assignmentRepository.save(any(DispatchAssignment.class))).thenAnswer(invocation -> {
            DispatchAssignment assignment = invocation.getArgument(0);
            setId(assignment, 20L);
            return assignment;
        });
        when(fleetService.getUnit(1L)).thenReturn(availableFleet);
        when(driverService.getDriver(1L)).thenReturn(availableDriver);
        when(routeService.getVersion(10L)).thenReturn(routeVersion);
    }

    @Test
    void validAssignment_retainsOriginalRouteVersionAndAppendsEvent() {
        stubRoute();
        stubSaveAndDisplay();

        DispatchAssignmentView result = service.createAssignment(command());

        assertEquals("SCHEDULED", result.dispatchStatus());
        assertEquals("ORIGINAL", result.routeCode());
        assertEquals(10L, result.routeVersionId());
        assertEquals(END.toString(), result.scheduledArrival());
        var event = ArgumentCaptor.forClass(DispatchEvent.class);
        verify(eventRepository).save(event.capture());
        assertNull(event.getValue().getPreviousStatus());
        assertEquals("SCHEDULED", event.getValue().getNewStatus());
        assertEquals(20L, event.getValue().getAssignmentId());
        var order = inOrder(fleetService, driverService, routeService);
        order.verify(fleetService).lockUnitForAssignment(1L);
        order.verify(driverService).lockDriverForAssignment(1L);
        order.verify(routeService).lockRouteForAssignment(1L);
    }

    @Test
    void unavailableVehicle_isRejectedBeforePersistence() {
        availableFleet = fleet("OUT_OF_SERVICE", null);
        stubFleet();
        assertRule(DispatchRule.VEHICLE_UNAVAILABLE, command());
        verifyNoInteractions(assignmentRepository, eventRepository);
    }

    @Test
    void maintenanceAvailability_isRejected() {
        availableFleet = fleet("MAINTENANCE", null);
        stubFleet();
        assertRule(DispatchRule.VEHICLE_MAINTENANCE_HOLD, command());
    }

    @Test
    void activeMaintenanceRecord_blocksOtherwiseAvailableVehicle() {
        stubFleet();
        when(maintenanceService.countActiveMaintenanceForUnit(1L)).thenReturn(1L);
        assertRule(DispatchRule.VEHICLE_MAINTENANCE_HOLD, command());
    }

    @Test
    void inspectionHold_isRejected() {
        availableFleet = fleet("INSPECTION_HOLD", null);
        stubFleet();
        assertRule(DispatchRule.VEHICLE_INSPECTION_HOLD, command());
    }

    @Test
    void archivedVehicle_isRejected() {
        availableFleet = fleet("AVAILABLE", START);
        stubFleet();
        assertRule(DispatchRule.VEHICLE_UNAVAILABLE, command());
    }

    @Test
    void unavailableDriver_isRejected() {
        availableDriver = driver("UNAVAILABLE", SERVICE_DATE.plusDays(1), null);
        stubDriver();
        assertRule(DispatchRule.DRIVER_UNAVAILABLE, command());
    }

    @Test
    void archivedDriver_isRejected() {
        availableDriver = driver("AVAILABLE", SERVICE_DATE.plusDays(1), START);
        stubDriver();
        assertRule(DispatchRule.DRIVER_UNAVAILABLE, command());
    }

    @Test
    void licenseExpiringBeforeFutureServiceDate_isRejected() {
        availableDriver = driver("AVAILABLE", SERVICE_DATE.minusDays(1), null);
        stubDriver();
        assertRule(DispatchRule.DRIVER_LICENSE_EXPIRED, command());
    }

    @Test
    void licenseValidThroughServiceDate_isAccepted() {
        availableDriver = driver("AVAILABLE", SERVICE_DATE, null);
        stubRoute();
        stubSaveAndDisplay();
        assertEquals("SCHEDULED", service.createAssignment(command()).dispatchStatus());
    }

    @Test
    void routeWithoutEffectiveVersion_isRejected() {
        stubDriver();
        when(routeService.lockRouteForAssignment(1L)).thenReturn(activeRoute);
        when(routeService.getEffectiveVersion(1L, SERVICE_DATE))
                .thenThrow(new CommandValidationException("No effective version"));
        assertRule(DispatchRule.ROUTE_VERSION_NOT_EFFECTIVE, command());
    }

    @Test
    void vehicleOverlap_returnsVehicleRule() {
        stubRoute();
        when(assignmentRepository.findFleetOverlaps(1L, START, END, null))
                .thenReturn(List.of(assignment()));
        var exception = assertThrows(DispatchConflictException.class,
                () -> service.createAssignment(command()));
        assertEquals(DispatchRule.VEHICLE_ASSIGNMENT_OVERLAP, exception.ruleCode());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void driverOverlap_returnsDriverRule() {
        stubRoute();
        when(assignmentRepository.findDriverOverlaps(1L, START, END, null))
                .thenReturn(List.of(assignment()));
        var exception = assertThrows(DispatchConflictException.class,
                () -> service.createAssignment(command()));
        assertEquals(DispatchRule.DRIVER_ASSIGNMENT_OVERLAP, exception.ruleCode());
    }

    @Test
    void equalDepartureAndArrival_isRejected() {
        assertRule(DispatchRule.INVALID_ASSIGNMENT_WINDOW,
                new CreateDispatchCommand(1L, 1L, 1L, SERVICE_DATE, START, START, null));
        verifyNoInteractions(fleetService, assignmentRepository);
    }

    @Test
    void departureOnDifferentServiceDate_isRejected() {
        assertRule(DispatchRule.INVALID_ASSIGNMENT_WINDOW,
                new CreateDispatchCommand(1L, 1L, 1L, SERVICE_DATE.minusDays(1), START, END, null));
    }

    @Test
    void omittedArrival_usesConfiguredDuration() {
        stubRoute();
        stubSaveAndDisplay();
        DispatchAssignmentView result = service.createAssignment(new CreateDispatchCommand(
                1L, 1L, 1L, SERVICE_DATE, START, null, null));
        assertEquals(END.toString(), result.scheduledArrival());
    }

    @Test
    void omittedArrivalWithoutConfiguredDuration_isRejected() {
        routeVersion = new RouteVersionView(10L, 1L, "ORIGINAL", "Route", null,
                null, SERVICE_DATE.minusMonths(1), null, false, List.of());
        stubRoute();
        assertRule(DispatchRule.INVALID_ASSIGNMENT_WINDOW,
                new CreateDispatchCommand(1L, 1L, 1L, SERVICE_DATE, START, null, null));
    }

    @Test
    void startRechecksAvailabilityWithoutChangingOriginalPlan() {
        DispatchAssignment assignment = assignment();
        when(assignmentRepository.findById(20L)).thenReturn(Optional.of(assignment));
        availableFleet = fleet("OUT_OF_SERVICE", null);
        stubFleet();

        assertThrows(DispatchValidationException.class, () -> service.startTrip(20L));
        assertEquals(DispatchStatus.SCHEDULED, assignment.getDispatchStatus());
        assertEquals(START, assignment.getScheduledDeparture());
        verifyNoInteractions(eventRepository);
    }

    @Test
    void lifecyclePreservesPlannedTimesAndAppendsTransitions() {
        DispatchAssignment assignment = assignment();
        when(assignmentRepository.findById(20L)).thenReturn(Optional.of(assignment));
        stubFleet();
        when(driverService.lockDriverForAssignment(1L)).thenReturn(availableDriver);
        when(routeService.lockRouteForAssignment(1L)).thenReturn(activeRoute);
        when(routeService.getVersion(10L)).thenReturn(routeVersion);
        when(fleetService.getUnit(1L)).thenReturn(availableFleet);
        when(driverService.getDriver(1L)).thenReturn(availableDriver);

        assertEquals("IN_PROGRESS", service.startTrip(20L).dispatchStatus());
        assertEquals("COMPLETED", service.completeTrip(20L).dispatchStatus());
        assertNotNull(assignment.getActualDeparture());
        assertNotNull(assignment.getActualArrival());
        assertEquals(START, assignment.getScheduledDeparture());
        assertEquals(END, assignment.getScheduledArrival());
        verify(eventRepository, times(2)).save(any());
        assertThrows(IllegalStateException.class, () -> service.cancelAssignment(20L));
        verify(eventRepository, times(2)).save(any());
    }

    @Test
    void searchAppliesDateAndStatusBeforePagination() {
        var pageable = PageRequest.of(0, 20);
        when(assignmentRepository.search(SERVICE_DATE, DispatchStatus.SCHEDULED, pageable))
                .thenReturn(Page.empty(pageable));
        assertTrue(service.searchAssignments(
                new DispatchQuery(SERVICE_DATE.toString(), "SCHEDULED"), pageable).isEmpty());
        verify(assignmentRepository).search(SERVICE_DATE, DispatchStatus.SCHEDULED, pageable);
    }

    @Test
    void configuredSchedule_createsIndependentTripInstance() {
        stubRoute();
        stubSaveAndDisplay();
        when(routeService.getSchedule(30L)).thenReturn(new com.transit.arctransit.route.RouteScheduleView(
                30L, 1L, "Morning service", java.time.LocalTime.of(8, 0),
                SERVICE_DATE.minusDays(1), null, true));
        var result = service.createAssignment(new CreateDispatchCommand(
                1L, 1L, 1L, SERVICE_DATE, START, END, null, 30L));
        assertEquals(30L, result.scheduleId());
        assertEquals(START.toString(), result.scheduledDeparture());
    }

    @Test
    void inactiveSchedule_isRejected() {
        stubScheduleValidation(new com.transit.arctransit.route.RouteScheduleView(
                30L, 1L, "Morning", java.time.LocalTime.of(8, 0),
                SERVICE_DATE.minusDays(1), null, false));
        assertScheduleRule();
    }

    @Test
    void scheduleFromDifferentRoute_isRejected() {
        stubScheduleValidation(new com.transit.arctransit.route.RouteScheduleView(
                30L, 2L, "Morning", java.time.LocalTime.of(8, 0),
                SERVICE_DATE.minusDays(1), null, true));
        assertScheduleRule();
    }

    @Test
    void scheduleExclusiveEndDate_isRejected() {
        stubScheduleValidation(new com.transit.arctransit.route.RouteScheduleView(
                30L, 1L, "Morning", java.time.LocalTime.of(8, 0),
                SERVICE_DATE.minusDays(1), SERVICE_DATE, true));
        assertScheduleRule();
    }

    @Test
    void departureDifferentFromSchedule_isRejected() {
        stubScheduleValidation(new com.transit.arctransit.route.RouteScheduleView(
                30L, 1L, "Morning", java.time.LocalTime.of(9, 0),
                SERVICE_DATE.minusDays(1), null, true));
        assertScheduleRule();
    }

    private void stubScheduleValidation(com.transit.arctransit.route.RouteScheduleView schedule) {
        stubDriver();
        when(routeService.lockRouteForAssignment(1L)).thenReturn(activeRoute);
        when(routeService.getSchedule(30L)).thenReturn(schedule);
    }

    private void assertScheduleRule() {
        assertRule(DispatchRule.SCHEDULE_NOT_APPLICABLE, new CreateDispatchCommand(
                1L, 1L, 1L, SERVICE_DATE, START, END, null, 30L));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void nonProfessionalLicense_isRejectedForBusAssignment() {
        availableDriver = new DriverView(1L, "EMP-001", "Juan", "Cruz", "LIC-001",
                "NON_PROFESSIONAL", SERVICE_DATE.plusYears(1).toString(), null,
                "ACTIVE", false, "AVAILABLE", null);
        stubDriver();
        assertRule(DispatchRule.DRIVER_QUALIFICATION_REQUIRED, command());
    }

    @Test
    void overdueTripStart_checksLicenseOnReportedStartDay() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Manila"));
        LocalDate plannedDate = today.minusDays(10);
        Instant departure = plannedDate.atTime(8, 0).atZone(java.time.ZoneId.of("Asia/Manila")).toInstant();
        DispatchAssignment overdue = new DispatchAssignment(1L, 1L, 1L,
                plannedDate, departure, departure.plusSeconds(3600), null);
        overdue.linkRouteVersion(10L);
        setId(overdue, 20L);
        availableDriver = driver("AVAILABLE", today.minusDays(1), null);
        when(assignmentRepository.findById(20L)).thenReturn(Optional.of(overdue));
        stubDriver();

        var exception = assertThrows(DispatchValidationException.class, () -> service.startTrip(20L));
        assertEquals(DispatchRule.DRIVER_LICENSE_EXPIRED, exception.ruleCode());
        assertEquals(DispatchStatus.SCHEDULED, overdue.getDispatchStatus());
        verifyNoInteractions(eventRepository);
    }

    @Test
    void startRejectsVehicleAlreadyReportedInProgressOutsidePlannedWindow() {
        DispatchAssignment assignment = assignment();
        when(assignmentRepository.findById(20L)).thenReturn(Optional.of(assignment));
        stubFleet();
        when(driverService.lockDriverForAssignment(1L)).thenReturn(availableDriver);
        when(routeService.lockRouteForAssignment(1L)).thenReturn(activeRoute);
        when(routeService.getVersion(10L)).thenReturn(routeVersion);
        when(assignmentRepository.existsByFleetUnitIdAndDispatchStatusAndIdNot(
                1L, DispatchStatus.IN_PROGRESS, 20L)).thenReturn(true);

        var exception = assertThrows(DispatchConflictException.class, () -> service.startTrip(20L));
        assertEquals(DispatchRule.VEHICLE_ASSIGNMENT_OVERLAP, exception.ruleCode());
        assertEquals(DispatchStatus.SCHEDULED, assignment.getDispatchStatus());
        verifyNoInteractions(eventRepository);
    }

    @Test
    void startRejectsDriverAlreadyReportedInProgressOutsidePlannedWindow() {
        DispatchAssignment assignment = assignment();
        when(assignmentRepository.findById(20L)).thenReturn(Optional.of(assignment));
        stubFleet();
        when(driverService.lockDriverForAssignment(1L)).thenReturn(availableDriver);
        when(routeService.lockRouteForAssignment(1L)).thenReturn(activeRoute);
        when(routeService.getVersion(10L)).thenReturn(routeVersion);
        when(assignmentRepository.existsByDriverIdAndDispatchStatusAndIdNot(
                1L, DispatchStatus.IN_PROGRESS, 20L)).thenReturn(true);

        var exception = assertThrows(DispatchConflictException.class, () -> service.startTrip(20L));
        assertEquals(DispatchRule.DRIVER_ASSIGNMENT_OVERLAP, exception.ruleCode());
        assertEquals(DispatchStatus.SCHEDULED, assignment.getDispatchStatus());
        verifyNoInteractions(eventRepository);
    }

    private void assertRule(DispatchRule expected, CreateDispatchCommand command) {
        var exception = assertThrows(DispatchValidationException.class,
                () -> service.createAssignment(command));
        assertEquals(expected, exception.ruleCode());
    }

    private DispatchAssignment assignment() {
        DispatchAssignment assignment = new DispatchAssignment(1L, 1L, 1L,
                SERVICE_DATE, START, END, null);
        assignment.linkRouteVersion(10L);
        setId(assignment, 20L);
        return assignment;
    }

    private static void setId(DispatchAssignment assignment, Long id) {
        try {
            var field = DispatchAssignment.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(assignment, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
