package com.transit.arctransit.reconciliation.application;

import com.transit.arctransit.audit.AuditRecordingService;
import com.transit.arctransit.dispatch.DispatchAssignmentView;
import com.transit.arctransit.dispatch.DispatchService;
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.driver.DriverView;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitView;
import com.transit.arctransit.incident.IncidentManagementService;
import com.transit.arctransit.maintenance.MaintenanceRecordView;
import com.transit.arctransit.maintenance.MaintenanceService;
import com.transit.arctransit.reconciliation.ReconciliationProperties;
import com.transit.arctransit.reconciliation.domain.*;
import com.transit.arctransit.reporting.OperationalReportService;
import com.transit.arctransit.reporting.OperationalReportView;
import com.transit.arctransit.reporting.ReportingProperties;
import com.transit.arctransit.common.exception.CommandValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AppReconciliationServiceTest {
    private OperationalExceptionRepository repository;
    private FleetManagementService fleet;
    private DriverManagementService drivers;
    private MaintenanceService maintenance;
    private DispatchService dispatch;
    private IncidentManagementService incidents;
    private OperationalReportService reports;
    private AppReconciliationService service;
    private List<OperationalException> detected;
    private Set<String> openKeys;

    @BeforeEach
    void setUp() {
        repository = mock(OperationalExceptionRepository.class);
        fleet = mock(FleetManagementService.class);
        drivers = mock(DriverManagementService.class);
        maintenance = mock(MaintenanceService.class);
        dispatch = mock(DispatchService.class);
        incidents = mock(IncidentManagementService.class);
        reports = mock(OperationalReportService.class);
        service = new AppReconciliationService(repository, new ReconciliationProperties(null, null),
                mock(AuditRecordingService.class), fleet, maintenance, dispatch, incidents,
                drivers, reports, new ReportingProperties(null, null));
        when(maintenance.searchRecords(any())).thenReturn(Page.empty());
        when(dispatch.searchAssignments(any(), any())).thenReturn(Page.empty());
        when(incidents.searchIncidents(any())).thenReturn(Page.empty());
        when(reports.getReviewCandidates(any())).thenReturn(List.of());
        detected = new ArrayList<>();
        openKeys = new HashSet<>();
        when(repository.existsByRuleCodeAndAffectedEntityTypeAndAffectedEntityIdAndExceptionStatusIn(
                anyString(), anyString(), anyLong(), anyList())).thenAnswer(invocation ->
                openKeys.contains(invocation.getArgument(0) + ":" + invocation.getArgument(1)
                        + ":" + invocation.getArgument(2)));
        when(repository.save(any())).thenAnswer(invocation -> {
            OperationalException exception = invocation.getArgument(0);
            detected.add(exception);
            openKeys.add(exception.getRuleCode() + ":" + exception.getAffectedEntityType()
                    + ":" + exception.getAffectedEntityId());
            return exception;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ordinaryAssignmentAndRecentActualDepartureAreNotFlagged() {
        configureAssignment("AVAILABLE", "AVAILABLE", "IN_PROGRESS",
                Instant.now().minusSeconds(6 * 3600), Instant.now().minusSeconds(60));
        service.runReconciliation();
        assertTrue(detected.isEmpty());
        verify(repository).lockForScan();
    }

    @Test
    void unavailableResourcesAndMaintenanceProduceIndependentReviewCases() {
        configureAssignment("OUT_OF_SERVICE", "UNAVAILABLE", "SCHEDULED", Instant.now(), null);
        when(maintenance.searchRecords(any())).thenReturn(new PageImpl<>(List.of(
                new MaintenanceRecordView(9L, 1L, "CORRECTIVE", "IN_PROGRESS", "Reported issue",
                        LocalDate.now(), Instant.now(), null, null, Instant.now()))));
        service.runReconciliation();
        assertEquals(Set.of("ASSIGNMENT_VEHICLE_UNAVAILABLE", "ASSIGNMENT_MAINTENANCE_HOLD",
                        "ASSIGNMENT_DRIVER_UNAVAILABLE"), ruleCodes());
        verify(fleet, never()).changeStatus(anyLong(), anyString());
    }

    @Test
    void inProgressMaintenanceWithAvailableVehicleIsAContradiction() {
        when(fleet.getUnit(1L)).thenReturn(unit("AVAILABLE"));
        when(maintenance.searchRecords(any())).thenReturn(new PageImpl<>(List.of(
                new MaintenanceRecordView(9L, 1L, "CORRECTIVE", "IN_PROGRESS", "Reported issue",
                        LocalDate.now(), Instant.now(), null, null, Instant.now()))));
        service.runReconciliation();
        assertEquals(Set.of("MAINTENANCE_AVAILABILITY_CONFLICT"), ruleCodes());
    }

    @Test
    void staleUnverifiedReportRetainsBothRulesWithoutDuplicatesAcrossScans() {
        when(reports.getReviewCandidates(any())).thenReturn(List.of(new OperationalReportView(
                7L, "RADIO", "staff", Instant.now().minusSeconds(3 * 3600),
                Instant.now().minusSeconds(3 * 3600), "UNVERIFIED", null, null,
                "FLEET", 1L, "Reported delay", "STALE", null, null)));
        service.runReconciliation();
        service.runReconciliation();
        assertEquals(2, detected.size());
        assertEquals(Set.of("UNVERIFIED_OPERATIONAL_REPORT", "STALE_OPERATIONAL_REPORT"), ruleCodes());
    }

    @Test
    void expiredQualificationAndInvalidVersionAreFlagged() {
        configureAssignment("AVAILABLE", "AVAILABLE", "SCHEDULED", Instant.now(), null);
        when(drivers.getDriver(2L)).thenReturn(new DriverView(2L, "D2", "First", "Last", "LICENSE",
                "PROFESSIONAL", LocalDate.now().minusDays(1).toString(), null,
                "ACTIVE", true, "AVAILABLE", null));
        DispatchAssignmentView invalid = new DispatchAssignmentView(3L, LocalDate.now().toString(),
                1L, "BUS1", 2L, "Driver", 4L, "R1", Instant.now().toString(), "SCHEDULED",
                5L, Instant.now().plusSeconds(3600).toString(), null, null,
                LocalDate.now().plusDays(1).toString(), null, false, null);
        when(dispatch.searchAssignments(any(), any())).thenReturn(new PageImpl<>(List.of(invalid)));
        service.runReconciliation();
        assertEquals(Set.of("ASSIGNMENT_DRIVER_LICENSE_EXPIRED", "ASSIGNMENT_ROUTE_VERSION_INVALID"), ruleCodes());
    }

    @Test
    void backendRejectsBlankResolutionNotesWithoutChangingCase() {
        OperationalException exception = new OperationalException(ExceptionType.CONTRADICTION,
                "RULE", "HIGH", "DISPATCH", 3L, "Conflict");
        exception.acknowledge("reviewer");
        exception.startInvestigation();
        when(repository.findById(1L)).thenReturn(Optional.of(exception));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("reviewer", null, List.of()));
        assertThrows(CommandValidationException.class, () -> service.resolveException(1L, " "));
        assertThrows(CommandValidationException.class, () -> service.dismissException(1L, null));
        assertEquals(ExceptionStatus.INVESTIGATING, exception.getExceptionStatus());
        assertNull(exception.getResolvedAt());
    }

    private void configureAssignment(String vehicleAvailability, String driverAvailability,
            String status, Instant planned, Instant actual) {
        when(fleet.getUnit(1L)).thenReturn(unit(vehicleAvailability));
        when(drivers.getDriver(2L)).thenReturn(new DriverView(2L, "D2", "First", "Last", "LICENSE",
                "PROFESSIONAL", LocalDate.now().plusYears(1).toString(), null,
                "ACTIVE", false, driverAvailability, null));
        DispatchAssignmentView assignment = new DispatchAssignmentView(3L, LocalDate.now().toString(),
                1L, "BUS1", 2L, "Driver", 4L, "R1", planned.toString(), status,
                5L, planned.plusSeconds(3600).toString(), actual == null ? null : actual.toString(), null,
                LocalDate.now().minusDays(1).toString(), null, false, null);
        when(dispatch.searchAssignments(any(), any())).thenReturn(new PageImpl<>(List.of(assignment)));
    }

    private static FleetUnitView unit(String availability) {
        return new FleetUnitView(1L, "BUS1", "PLATE", "BUS", (short) 40,
                "ACTIVE", "ACTIVE", availability, null);
    }

    private Set<String> ruleCodes() {
        return detected.stream().map(OperationalException::getRuleCode)
                .collect(java.util.stream.Collectors.toSet());
    }
}
