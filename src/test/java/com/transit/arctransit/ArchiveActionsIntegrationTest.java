package com.transit.arctransit;

import com.transit.arctransit.auth.*;
import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.dispatch.*;
import com.transit.arctransit.driver.*;
import com.transit.arctransit.fleet.*;
import com.transit.arctransit.incident.*;
import com.transit.arctransit.maintenance.*;
import com.transit.arctransit.reporting.*;
import com.transit.arctransit.route.*;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ARC_TEST_POSTGRES", matches = "true")
class ArchiveActionsIntegrationTest {
    @Autowired DispatchService dispatch;
    @Autowired FleetManagementService fleet;
    @Autowired DriverManagementService drivers;
    @Autowired RouteManagementService routes;
    @Autowired UserAdministrationService users;
    @Autowired IncidentManagementService incidents;
    @Autowired MaintenanceService maintenance;
    @Autowired OperationalReportService reports;
    @Autowired UserDetailsService logins;
    @Autowired JdbcTemplate jdbc;
    @Value("${arc.security.admin.username}") String administrator;

    @BeforeEach
    void authenticate() { as(administrator, "SYSTEM_ADMIN"); }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void deletedArchivedDispatchRetainsEventsAndAuditSnapshot() {
        var trip = trip();
        assertThrows(BusinessConflictException.class, () -> dispatch.hardDeleteAssignment(trip.id()));
        dispatch.cancelAssignment(trip.id());
        dispatch.archiveAssignment(trip.id());
        long events = count("SELECT count(*) FROM arc.dispatch_events WHERE assignment_id=?", trip.id());
        dispatch.hardDeleteAssignment(trip.id());
        assertEquals(0, count("SELECT count(*) FROM arc.dispatch_assignments WHERE id=?", trip.id()));
        assertEquals(events, count("SELECT count(*) FROM arc.dispatch_events WHERE assignment_id=?", trip.id()));
        String snapshot = jdbc.queryForObject("SELECT details FROM arc.audit_logs WHERE action_name='DISPATCH_DELETED' AND entity_id=?",
                String.class, trip.id());
        assertTrue(snapshot.contains(trip.fleetUnitNumber()));
        assertTrue(snapshot.contains("Archive workflow sample notes"));
        assertEquals(1, count("SELECT count(*) FROM arc.audit_logs WHERE action_name='DISPATCH_ASSIGNED' AND entity_id=?", trip.id()));
        assertThrows(RuntimeException.class, () -> jdbc.update(
                "UPDATE arc.dispatch_events SET actor='changed' WHERE assignment_id=?", trip.id()));
        assertThrows(RuntimeException.class, () -> jdbc.update("""
                INSERT INTO arc.dispatch_events(assignment_id,new_status,effective_at,recorded_at,actor,source_type)
                VALUES (?,'CANCELLED',now(),now(),'probe','STAFF_ENTRY')
                """, trip.id()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"incident", "report", "exception"})
    void linkedOperationalRecordsBlockDispatchDeletion(String type) {
        var trip = trip();
        switch (type) {
            case "incident" -> incidents.reportIncident(new CreateIncidentCommand("Trip issue", null, "LOW",
                    "DISPATCH", trip.id(), Instant.now().minusSeconds(1), null));
            case "report" -> reports.submitReport(new CreateOperationalReportCommand("PHONE", "DISPATCH", trip.id(),
                    Instant.now().minusSeconds(1), "Staff reported a trip concern"));
            case "exception" -> jdbc.update("""
                    INSERT INTO arc.operational_exceptions(exception_type,severity,affected_entity_type,affected_entity_id,
                        description,exception_status,detected_at,rule_code)
                    VALUES ('CONTRADICTION','LOW','DISPATCH',?,'Sample retained case','RESOLVED',now(),'MANUAL_REVIEW')
                    """, trip.id());
        }
        dispatch.cancelAssignment(trip.id());
        dispatch.archiveAssignment(trip.id());
        assertThrows(BusinessConflictException.class, () -> dispatch.hardDeleteAssignment(trip.id()));
        assertEquals(1, count("SELECT count(*) FROM arc.dispatch_assignments WHERE id=?", trip.id()));
        assertEquals(0, audit("DISPATCH_DELETED", trip.id()));
    }

    @Test
    void deletedArchivedAccountCannotLoginAndAllPriorUsernamesRemainReserved() {
        var original = staff();
        assertThrows(BusinessConflictException.class, () -> users.hardDeleteUser(original.username()));
        var renamed = users.updateUser(new UpdateUserCommand(original.id(), original.version(), name(),
                "Renamed Sample Staff", null, original.roles()));
        users.deleteUser(renamed.username());
        users.hardDeleteUser(renamed.username());
        assertEquals(0, count("SELECT count(*) FROM arc.app_users WHERE id=?", original.id()));
        assertEquals(0, count("SELECT count(*) FROM arc.user_roles WHERE user_id=?", original.id()));
        assertEquals(2, count("SELECT count(*) FROM arc.username_reservations WHERE user_id=?", original.id()));
        assertEquals(1, audit("USER_DELETED", original.id()));
        assertTrue(audit("USER_CREATED", original.id()) > 0);
        assertThrows(UsernameNotFoundException.class, () -> logins.loadUserByUsername(renamed.username()));
        for (String username : List.of(original.username(), renamed.username())) {
            assertThrows(BusinessConflictException.class, () -> users.createUser(new CreateUserCommand(username,
                    "Arctest899!@#", "Replacement", null, Set.of("OPERATIONS_STAFF"))));
        }
        assertThrows(RuntimeException.class, () -> jdbc.update(
                "UPDATE arc.username_reservations SET username='changed' WHERE user_id=?", original.id()));
        assertThrows(RuntimeException.class, () -> jdbc.update(
                "INSERT INTO arc.username_reservations(username,user_id) VALUES (?,?)", name(), original.id()));
    }

    @Test
    void accountRoleGrantReferencesAndDefaultAdministratorRemainProtected() {
        var grantor = users.createUser(new CreateUserCommand(name(), "Arctest899!@#", "Sample Admin", null, Set.of("SYSTEM_ADMIN")));
        as(grantor.username(), "SYSTEM_ADMIN");
        staff();
        authenticate();
        users.deleteUser(grantor.username());
        assertTrue(assertThrows(BusinessConflictException.class, () -> users.hardDeleteUser(grantor.username()))
                .getMessage().contains("granted roles"));
        assertThrows(BusinessConflictException.class, () -> users.hardDeleteUser(administrator));
        assertEquals(1, count("SELECT count(*) FROM arc.app_users WHERE id=?", grantor.id()));
    }

    @Test
    void unlinkRequiresReasonAndExpectedLinkThenPreservesBothRecordsAndHistory() {
        var trip = trip();
        var incident = incidents.reportIncident(new CreateIncidentCommand("Door issue", null, "MEDIUM", "FLEET",
                trip.fleetUnitId(), Instant.now().minusSeconds(1), null));
        var repair = maintenance.requestMaintenance(new CreateMaintenanceCommand(trip.fleetUnitId(), "CORRECTIVE", "Inspect door", null));
        incidents.linkMaintenanceRecord(incident.id(), repair.id());
        assertThrows(ConstraintViolationException.class, () -> incidents.unlinkMaintenanceRecord(incident.id(), repair.id(), " "));
        assertThrows(BusinessConflictException.class, () -> incidents.unlinkMaintenanceRecord(incident.id(), repair.id() + 1, "Wrong expected link"));
        var updated = incidents.unlinkMaintenanceRecord(incident.id(), repair.id(), "Linked the wrong repair request.");
        assertNull(updated.maintenanceRecordId());
        assertEquals(incident.status(), updated.status());
        assertEquals(repair.id(), maintenance.getRecord(repair.id()).id());
        assertEquals(3, incidents.getHistory(incident.id()).size());
        assertEquals(1, audit("INCIDENT_MAINTENANCE_UNLINKED", incident.id()));
        assertThrows(BusinessConflictException.class, () -> incidents.unlinkMaintenanceRecord(incident.id(), repair.id(), "Repeated request"));
        assertEquals(repair.id(), incidents.linkMaintenanceRecord(incident.id(), repair.id()).maintenanceRecordId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"dispatch", "user", "unlink"})
    void failedAuditRollsBackDeletionOrUnlink(String operation) {
        var trip = trip();
        dispatch.cancelAssignment(trip.id());
        dispatch.archiveAssignment(trip.id());
        var user = staff();
        users.deleteUser(user.username());
        var incident = incidents.reportIncident(new CreateIncidentCommand("Door concern", null, "LOW", "FLEET", trip.fleetUnitId()));
        var repair = maintenance.requestMaintenance(new CreateMaintenanceCommand(trip.fleetUnitId(), "CORRECTIVE", "Inspect mirror", null));
        incidents.linkMaintenanceRecord(incident.id(), repair.id());
        jdbc.execute("""
                CREATE FUNCTION arc.reject_archive_action_audit() RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.action_name IN ('DISPATCH_DELETED','USER_DELETED','INCIDENT_MAINTENANCE_UNLINKED') THEN
                        RAISE EXCEPTION 'Test audit unavailable' USING ERRCODE='23514';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """);
        jdbc.execute("CREATE TRIGGER reject_archive_action_audit BEFORE INSERT ON arc.audit_logs "
                + "FOR EACH ROW EXECUTE FUNCTION arc.reject_archive_action_audit()");
        try {
            assertThrows(RuntimeException.class, () -> {
                switch (operation) {
                    case "dispatch" -> dispatch.hardDeleteAssignment(trip.id());
                    case "user" -> users.hardDeleteUser(user.username());
                    case "unlink" -> incidents.unlinkMaintenanceRecord(incident.id(), repair.id(), "Correction");
                }
            });
            assertEquals(1, count("SELECT count(*) FROM arc.dispatch_assignments WHERE id=?", trip.id()));
            assertEquals(1, count("SELECT count(*) FROM arc.app_users WHERE id=?", user.id()));
            assertEquals(1, count("SELECT count(*) FROM arc.user_roles WHERE user_id=?", user.id()));
            assertEquals(repair.id(), jdbc.queryForObject("SELECT maintenance_record_id FROM arc.incidents WHERE id=?", Long.class, incident.id()));
            assertEquals(2, incidents.getHistory(incident.id()).size());
        } finally {
            jdbc.execute("DROP TRIGGER reject_archive_action_audit ON arc.audit_logs");
            jdbc.execute("DROP FUNCTION arc.reject_archive_action_audit()");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"+639691477344", "0969147734", "096914773444", "19691477344", "09691477abc", "09 91477344", " "})
    void invalidLocalMobileIsRejectedForCreateUpdateAndSql(String mobile) {
        assertThrows(ConstraintViolationException.class, () -> drivers.registerDriver(driverCommand(mobile)));
        var saved = drivers.registerDriver(driverCommand("09691477344"));
        assertThrows(ConstraintViolationException.class, () -> drivers.updateDriver(new UpdateDriverCommand(saved.id(),
                saved.employeeNumber(), saved.firstName(), saved.lastName(), saved.licenseNumber(), saved.licenseType(),
                LocalDate.parse(saved.licenseExpiryDate()), mobile)));
        assertThrows(RuntimeException.class, () -> jdbc.update("UPDATE arc.drivers SET contact_number=? WHERE id=?", mobile, saved.id()));
        assertEquals("09691477344", drivers.getDriver(saved.id()).contactNumber());
    }

    @Test
    void optionalMobileAndValidLocalMobileSaveAndStaffCannotDeleteOrUnlink() {
        assertNull(drivers.registerDriver(driverCommand(null)).contactNumber());
        assertNull(drivers.registerDriver(driverCommand("")).contactNumber());
        assertEquals("09691477344", drivers.registerDriver(driverCommand("09691477344")).contactNumber());
        as("sample-staff", "OPERATIONS_STAFF");
        assertThrows(AccessDeniedException.class, () -> dispatch.hardDeleteAssignment(1L));
        assertThrows(AccessDeniedException.class, () -> users.hardDeleteUser("sample-user"));
        assertThrows(AccessDeniedException.class, () -> incidents.unlinkMaintenanceRecord(1L, 1L, "Correction"));
    }

    private DispatchAssignmentView trip() {
        String code = name().toUpperCase();
        var bus = fleet.createUnit(new CreateFleetUnitCommand(code, "P-" + code, (short) 40));
        fleet.changeAdministrativeStatus(bus.id(), "ACTIVE");
        fleet.changeAvailability(new ChangeVehicleAvailabilityCommand(bus.id(), "AVAILABLE", Instant.now(), "Sample fixture"));
        var driver = drivers.registerDriver(driverCommand(null));
        drivers.changeAvailability(new ChangeDriverAvailabilityCommand(driver.id(), "AVAILABLE", Instant.now(), "Sample fixture"));
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Manila")).plusDays(3);
        var route = routes.createRoute(new CreateRouteCommand(code, "Sample Route", null, 45, List.of(), date));
        routes.changeRouteStatus(route.id(), "ACTIVE");
        Instant departure = date.atTime(8, 0).atZone(ZoneId.of("Asia/Manila")).toInstant();
        return dispatch.createAssignment(new CreateDispatchCommand(bus.id(), driver.id(), route.id(), date,
                departure, departure.plusSeconds(2700), "Archive workflow sample notes"));
    }

    @Test
    void reportPickerSearchesDispatchByBusAndOmitsArchivedTrips() {
        var trip = trip();
        as("report-picker-staff", "OPERATIONS_STAFF");
        var matches = dispatch.searchReportCandidates(trip.fleetUnitNumber(),
                org.springframework.data.domain.PageRequest.of(0, 1, org.springframework.data.domain.Sort.by("id")));
        assertEquals(1, matches.getTotalElements());
        assertEquals(trip.id(), matches.getContent().getFirst().id());
        as(administrator, "SYSTEM_ADMIN");
        dispatch.cancelAssignment(trip.id());
        dispatch.archiveAssignment(trip.id());
        assertEquals(0, dispatch.searchReportCandidates(trip.fleetUnitNumber(),
                org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements());
    }

    private CreateDriverCommand driverCommand(String mobile) {
        String code = name().toUpperCase();
        return new CreateDriverCommand(code, "Sample", "Driver", "L-" + code,
                "PROFESSIONAL", LocalDate.now().plusYears(2), mobile);
    }

    private UserView staff() {
        return users.createUser(new CreateUserCommand(name(), "Arctest899!@#", "Sample Staff", null, Set.of("OPERATIONS_STAFF")));
    }

    private long audit(String action, Long id) {
        return count("SELECT count(*) FROM arc.audit_logs WHERE action_name=? AND entity_id=?", action, id);
    }

    private long count(String query, Object... values) { return jdbc.queryForObject(query, Long.class, values); }
    private static String name() { return "a19-" + UUID.randomUUID().toString().substring(0, 8); }
    private static void as(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
