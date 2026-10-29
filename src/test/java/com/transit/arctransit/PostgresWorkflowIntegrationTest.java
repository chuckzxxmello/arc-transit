package com.transit.arctransit;

import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.dispatch.CreateDispatchCommand;
import com.transit.arctransit.dispatch.DispatchAssignmentView;
import com.transit.arctransit.dispatch.DispatchService;
import com.transit.arctransit.driver.ChangeDriverAvailabilityCommand;
import com.transit.arctransit.driver.CreateDriverCommand;
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.fleet.ChangeVehicleAvailabilityCommand;
import com.transit.arctransit.fleet.CreateFleetUnitCommand;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.incident.CreateIncidentCommand;
import com.transit.arctransit.incident.IncidentManagementService;
import com.transit.arctransit.reporting.CreateOperationalReportCommand;
import com.transit.arctransit.reporting.OperationalReportService;
import com.transit.arctransit.route.CreateRouteCommand;
import com.transit.arctransit.route.RouteManagementService;
import com.transit.arctransit.route.UpdateRouteCommand;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ARC_TEST_POSTGRES", matches = "true")
class PostgresWorkflowIntegrationTest {

    @Autowired private FleetManagementService fleetService;
    @Autowired private DriverManagementService driverService;
    @Autowired private RouteManagementService routeService;
    @Autowired private DispatchService dispatchService;
    @Autowired private OperationalReportService reportService;
    @Autowired private IncidentManagementService incidentService;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void authenticate() {
        authenticateAs("integration-admin", "SYSTEM_ADMIN");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void availabilityAndHistoryCommitTogetherAndRollbackOnHistoryFailure() {
        Long fleetId = createFleet();
        long previousCount = fleetService.availabilityHistory(fleetId, PageRequest.of(0, 10)).getTotalElements();
        fleetService.changeAvailability(new ChangeVehicleAvailabilityCommand(
                fleetId, "INSPECTION_HOLD", Instant.now(), "Inspection report received"));
        assertEquals("INSPECTION_HOLD", fleetService.getUnit(fleetId).availability());
        assertEquals(previousCount + 1,
                fleetService.availabilityHistory(fleetId, PageRequest.of(0, 10)).getTotalElements());
        jdbc.execute("""
                CREATE FUNCTION arc.reject_availability_probe() RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.reason = 'ROLLBACK_PROBE' THEN RAISE EXCEPTION 'Test history failure'; END IF;
                    RETURN NEW;
                END;
                $$
                """);
        jdbc.execute("CREATE TRIGGER reject_availability_probe BEFORE INSERT ON arc.vehicle_availability_history "
                + "FOR EACH ROW EXECUTE FUNCTION arc.reject_availability_probe()");
        try {
            assertThrows(RuntimeException.class, () -> fleetService.changeAvailability(
                    new ChangeVehicleAvailabilityCommand(fleetId, "AVAILABLE", Instant.now(), "ROLLBACK_PROBE")));
            assertEquals("INSPECTION_HOLD", fleetService.getUnit(fleetId).availability());
            assertEquals(previousCount + 1,
                    fleetService.availabilityHistory(fleetId, PageRequest.of(0, 10)).getTotalElements());
        } finally {
            jdbc.execute("DROP TRIGGER reject_availability_probe ON arc.vehicle_availability_history");
            jdbc.execute("DROP FUNCTION arc.reject_availability_probe()");
        }
    }

    @Test
    void routePublicationPreservesHistoricalAssignmentAndStopDefinitions() {
        Fixture fixture = fixture();
        DispatchAssignmentView first = dispatchService.createAssignment(command(fixture, 8, 9));
        LocalDate nextVersionDate = fixture.date().plusDays(1);
        routeService.updateRoute(new UpdateRouteCommand(fixture.routeId(), fixture.routeCode(),
                "Revised Route", "Version two", 90,
                List.of(new CreateRouteCommand.StopEntry("Revised Terminal", 1, 0)), nextVersionDate, null));
        var oldVersion = routeService.getVersion(first.routeVersionId());
        assertEquals("Original Terminal", oldVersion.stops().getFirst().stopName());
        assertEquals(first.routeVersionId(), dispatchService.getAssignment(first.id()).routeVersionId());
        DispatchAssignmentView second = dispatchService.createAssignment(new CreateDispatchCommand(
                fixture.fleetId(), fixture.driverId(), fixture.routeId(), nextVersionDate,
                at(nextVersionDate, 8), at(nextVersionDate, 10), null));
        assertNotEquals(first.routeVersionId(), second.routeVersionId());
        assertEquals("Revised Terminal", routeService.getVersion(second.routeVersionId()).stops().getFirst().stopName());
        assertThrows(RuntimeException.class, () -> routeService.updateRoute(new UpdateRouteCommand(
                fixture.routeId(), fixture.routeCode(), "Overlap", null, 60, List.of(),
                fixture.date(), fixture.date().plusDays(2))));
    }

    @Test
    void adjacentIntervalsSucceedAndOvernightIntervalsConflict() {
        Fixture fixture = fixture();
        dispatchService.createAssignment(command(fixture, 8, 9));
        dispatchService.createAssignment(command(fixture, 9, 10));
        assertThrows(RuntimeException.class, () -> dispatchService.createAssignment(command(fixture, 8, 10)));
        LocalDate followingDate = fixture.date().plusDays(1);
        dispatchService.createAssignment(new CreateDispatchCommand(fixture.fleetId(), fixture.driverId(),
                fixture.routeId(), fixture.date(), at(fixture.date(), 23), at(followingDate, 2), null));
        assertThrows(RuntimeException.class, () -> dispatchService.createAssignment(new CreateDispatchCommand(
                fixture.fleetId(), fixture.driverId(), fixture.routeId(), followingDate,
                at(followingDate, 1), at(followingDate, 3), null)));
    }

    @Test
    void simultaneousOverlappingAssignmentsCannotBothCommit() throws Exception {
        Fixture fixture = fixture();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> attemptConcurrent(fixture, start, 8, 10));
            var second = executor.submit(() -> attemptConcurrent(fixture, start, 9, 11));
            start.countDown();
            assertEquals(1, first.get(30, TimeUnit.SECONDS) + second.get(30, TimeUnit.SECONDS));
        }
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM arc.dispatch_assignments WHERE fleet_unit_id = ?",
                Long.class, fixture.fleetId()));
    }

    @Test
    void onlyOneAdjacentPlannedTripCanBeRecordedInProgress() {
        Fixture fixture = fixture();
        var first = dispatchService.createAssignment(command(fixture, 8, 9));
        var second = dispatchService.createAssignment(command(fixture, 9, 10));
        dispatchService.startTrip(first.id());
        assertThrows(com.transit.arctransit.common.exception.BusinessConflictException.class,
                () -> dispatchService.startTrip(second.id()));
        assertEquals("SCHEDULED", dispatchService.getAssignment(second.id()).dispatchStatus());
        dispatchService.completeTrip(first.id());
        assertEquals("IN_PROGRESS", dispatchService.startTrip(second.id()).dispatchStatus());
    }

    @Test
    void configuredTripStartAndCancellationDoNotDeadlock() throws Exception {
        Fixture fixture = fixture();
        var schedule = routeService.createSchedule(new com.transit.arctransit.route.CreateRouteScheduleCommand(
                fixture.routeId(), "Morning", LocalTime.of(8, 0), fixture.date(), null));
        var assignment = dispatchService.createAssignment(new CreateDispatchCommand(fixture.fleetId(),
                fixture.driverId(), fixture.routeId(), fixture.date(), at(fixture.date(), 8),
                at(fixture.date(), 9), null, schedule.id()));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var begin = executor.submit(() -> {
                attemptLifecycle(assignment.id(), true, start);
                return null;
            });
            var cancel = executor.submit(() -> {
                attemptLifecycle(assignment.id(), false, start);
                return null;
            });
            start.countDown();
            begin.get(30, TimeUnit.SECONDS);
            cancel.get(30, TimeUnit.SECONDS);
        }
        var current = dispatchService.getAssignment(assignment.id());
        assertTrue(List.of("CANCELLED", "IN_PROGRESS").contains(current.dispatchStatus()));
        assertEquals(current.dispatchStatus(), dispatchService.getHistory(assignment.id()).getLast().newStatus());
        assertEquals(schedule.id(), current.scheduleId());
    }

    @Test
    void historicalRecordsRejectDirectDatabaseMutation() {
        Fixture fixture = fixture();
        var assignment = dispatchService.createAssignment(command(fixture, 8, 9));
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update(
                "UPDATE arc.vehicle_availability_history SET reason = 'tampered' WHERE fleet_unit_id = ?",
                fixture.fleetId()));
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update(
                "UPDATE arc.route_version_stops SET stop_name = 'tampered' WHERE route_version_id = ?",
                assignment.routeVersionId()));
        assertThrows(org.springframework.dao.DataAccessException.class, () -> jdbc.update(
                "INSERT INTO arc.route_version_stops(route_version_id, stop_name, stop_sequence) VALUES (?, 'Added later', 2)",
                assignment.routeVersionId()));
        assertEquals("Original Terminal", routeService.getVersion(assignment.routeVersionId()).stops().getFirst().stopName());
    }

    @Test
    void operationalReportRetainsProvenanceAndUsesProtectedVerificationBoundary() {
        Long fleetId = createFleet();
        Instant effectiveAt = Instant.now().minusSeconds(600);
        authenticateAs("reporting-staff", "OPERATIONS_STAFF");
        var report = reportService.submitReport(new CreateOperationalReportCommand(
                "RADIO", "FLEET", fleetId, effectiveAt, "Staff reported an inspection concern"));
        assertEquals("reporting-staff", report.reportedBy());
        assertEquals("RADIO", report.sourceType());
        assertEquals(effectiveAt.toEpochMilli(), report.effectiveAt().toEpochMilli());
        assertFalse(report.reportedAt().isBefore(report.effectiveAt()));
        assertEquals("UNVERIFIED", report.verificationStatus());
        assertThrows(AccessDeniedException.class,
                () -> reportService.verifyReport(report.id(), "CONFIRMED", "Reviewed supporting report"));
        assertThrows(ConstraintViolationException.class, () -> reportService.submitReport(
                new CreateOperationalReportCommand("", "FLEET", fleetId, null, "")));
        authenticate();
        var reviewed = reportService.verifyReport(report.id(), "DISPUTED", "Conflicting staff reports");
        assertEquals("integration-admin", reviewed.verifiedBy());
        assertEquals(2, reportService.getVerificationHistory(report.id()).size());
        var incident = incidentService.reportIncident(new CreateIncidentCommand("Inspection concern",
                "Reported condition requires investigation", "MEDIUM", "FLEET", fleetId,
                effectiveAt, report.id()));
        assertEquals(report.id(), incident.operationalReportId());
        assertEquals(1, incidentService.getHistory(incident.id()).size());
    }

    @Test
    void reportVerificationRollsBackWhenItsHistoryCannotBeWritten() {
        var report = reportService.submitReport(new CreateOperationalReportCommand("STAFF_ENTRY", "FLEET",
                createFleet(), Instant.now().minusSeconds(1), "Reported issue"));
        jdbc.execute("""
                CREATE FUNCTION arc.reject_report_probe() RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.reason = 'ROLLBACK_PROBE' THEN RAISE EXCEPTION 'Test verification failure'; END IF;
                    RETURN NEW;
                END;
                $$
                """);
        jdbc.execute("CREATE TRIGGER reject_report_probe BEFORE INSERT ON arc.report_verification_events "
                + "FOR EACH ROW EXECUTE FUNCTION arc.reject_report_probe()");
        try {
            assertThrows(RuntimeException.class, () -> reportService.verifyReport(
                    report.id(), "CONFIRMED", "ROLLBACK_PROBE"));
            assertEquals("UNVERIFIED", reportService.getReport(report.id()).verificationStatus());
            assertEquals(1, reportService.getVerificationHistory(report.id()).size());
        } finally {
            jdbc.execute("DROP TRIGGER reject_report_probe ON arc.report_verification_events");
            jdbc.execute("DROP FUNCTION arc.reject_report_probe()");
        }
    }

    private int attemptConcurrent(Fixture fixture, CountDownLatch start, int from, int to) throws Exception {
        authenticate();
        try {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent test did not start.");
            }
            dispatchService.createAssignment(command(fixture, from, to));
            return 1;
        } catch (CommandValidationException | com.transit.arctransit.common.exception.BusinessConflictException expected) {
            return 0;
        } finally {
            clearAuthentication();
        }
    }

    private void attemptLifecycle(Long assignmentId, boolean begin, CountDownLatch start) throws Exception {
        authenticate();
        try {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent lifecycle test did not start.");
            }
            if (begin) {
                dispatchService.startTrip(assignmentId);
            } else {
                dispatchService.cancelAssignment(assignmentId);
            }
        } catch (org.springframework.dao.OptimisticLockingFailureException | IllegalStateException expectedConflict) {
            // A simultaneous state transition may invalidate a previously read version.
        } finally {
            clearAuthentication();
        }
    }

    private Long createFleet() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Long id = fleetService.createUnit(new CreateFleetUnitCommand("TEST-" + suffix, "P-" + suffix, (short) 40)).id();
        fleetService.changeAdministrativeStatus(id, "ACTIVE");
        fleetService.changeAvailability(new ChangeVehicleAvailabilityCommand(id, "AVAILABLE", Instant.now(), "Test fixture"));
        return id;
    }

    private Fixture fixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Long fleetId = createFleet();
        Long driverId = driverService.registerDriver(new CreateDriverCommand("DRV-" + suffix, "Test", "Driver",
                "LIC-" + suffix, "PROFESSIONAL", LocalDate.now().plusYears(2), null)).id();
        driverService.changeAvailability(new ChangeDriverAvailabilityCommand(driverId, "AVAILABLE", Instant.now(), "Test fixture"));
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Manila")).plusDays(5);
        String routeCode = "RT-" + suffix;
        Long routeId = routeService.createRoute(new CreateRouteCommand(routeCode, "Original Route", null, 60,
                List.of(new CreateRouteCommand.StopEntry("Original Terminal", 1, 0)), date)).id();
        return new Fixture(fleetId, driverId, routeId, routeCode, date);
    }

    private static CreateDispatchCommand command(Fixture fixture, int from, int to) {
        return new CreateDispatchCommand(fixture.fleetId(), fixture.driverId(), fixture.routeId(), fixture.date(),
                at(fixture.date(), from), at(fixture.date(), to), null);
    }

    private static Instant at(LocalDate date, int hour) {
        return date.atTime(LocalTime.of(hour, 0)).atZone(ZoneId.of("Asia/Manila")).toInstant();
    }

    private static void authenticateAs(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private record Fixture(Long fleetId, Long driverId, Long routeId, String routeCode, LocalDate date) {
    }
}
