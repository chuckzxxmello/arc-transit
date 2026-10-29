package com.transit.arctransit;

import com.transit.arctransit.auth.*;
import com.transit.arctransit.dispatch.*;
import com.transit.arctransit.driver.*;
import com.transit.arctransit.fleet.*;
import com.transit.arctransit.incident.*;
import com.transit.arctransit.maintenance.*;
import com.transit.arctransit.reconciliation.ReconciliationService;
import com.transit.arctransit.reporting.*;
import com.transit.arctransit.route.*;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "ARC_TEST_POSTGRES", matches = "true")
class InputValidationIntegrationTest {
    @Autowired UserAdministrationService users;
    @Autowired FleetManagementService fleet;
    @Autowired DriverManagementService drivers;
    @Autowired RouteManagementService routes;
    @Autowired DispatchService dispatch;
    @Autowired MaintenanceService maintenance;
    @Autowired IncidentManagementService incidents;
    @Autowired OperationalReportService reports;
    @Autowired ReconciliationService reconciliation;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "validation-test", null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsOverlongCommandsBeforeLookingUpOrWritingRecords() {
        String longText = "r".repeat(501);
        List<Executable> attempts = List.of(
                () -> users.createUser(new CreateUserCommand("r".repeat(51), "Valid899!", "Staff", null,
                        Set.of("OPERATIONS_STAFF"))),
                () -> fleet.createUnit(new CreateFleetUnitCommand("r".repeat(31), "ABC123", (short) 40)),
                () -> drivers.registerDriver(new CreateDriverCommand("DRV1", "r".repeat(51), "Driver", "LIC1",
                        "PROFESSIONAL", LocalDate.now().plusYears(1), null)),
                () -> routes.createRoute(new CreateRouteCommand("RT1", "r".repeat(101), null, 60, List.of())),
                () -> routes.createSchedule(new CreateRouteScheduleCommand(1L, "r".repeat(101), LocalTime.NOON,
                        LocalDate.now(), null)),
                () -> dispatch.createAssignment(new CreateDispatchCommand(1L, 1L, 1L, LocalDate.now(),
                        Instant.now(), null, longText)),
                () -> maintenance.requestMaintenance(new CreateMaintenanceCommand(1L, "PREVENTIVE", longText, null)),
                () -> incidents.reportIncident(new CreateIncidentCommand("r".repeat(201), null, "MEDIUM", null, null)),
                () -> reports.submitReport(new CreateOperationalReportCommand("RADIO", "FLEET", 1L,
                        Instant.now().minusSeconds(1), longText)),
                () -> fleet.changeAvailability(new ChangeVehicleAvailabilityCommand(1L, "AVAILABLE", Instant.now(), longText)),
                () -> drivers.changeAvailability(new ChangeDriverAvailabilityCommand(1L, "AVAILABLE", Instant.now(), longText)));
        assertAll(attempts.stream().map(attempt -> (Executable) () ->
                assertThrows(ConstraintViolationException.class, attempt)));
    }

    @Test
    void rejectsOverlongWorkflowNotesAndNonpositiveIds() {
        String notes = "r".repeat(501);
        List<Executable> attempts = List.of(
                () -> maintenance.completeMaintenance(1L, notes),
                () -> incidents.resolveIncident(1L, notes),
                () -> reports.verifyReport(1L, "CONFIRMED", notes),
                () -> reconciliation.resolveException(1L, notes),
                () -> reconciliation.dismissException(1L, notes),
                () -> fleet.getUnit(0L), () -> drivers.getDriver(-1L), () -> routes.getRoute(0L),
                () -> dispatch.getAssignment(-1L), () -> maintenance.getRecord(null));
        assertAll(attempts.stream().map(attempt -> (Executable) () ->
                assertThrows(ConstraintViolationException.class, attempt)));
    }

    @Test
    void rejectsControlCharactersAndUnboundedSearches() {
        List<Executable> attempts = List.of(
                () -> incidents.reportIncident(new CreateIncidentCommand("Bad" + (char) 0 + "title", null, "MEDIUM", null, null)),
                () -> users.searchUsers(new UserQuery("r".repeat(201)), PageRequest.of(0, 10)),
                () -> fleet.searchUnits(new FleetUnitQuery("r".repeat(201), null), PageRequest.of(0, 10)),
                () -> drivers.searchDrivers(new DriverQuery("r".repeat(201), null), PageRequest.of(0, 10)),
                () -> routes.searchRoutes(new RouteQuery("r".repeat(201), null), PageRequest.of(0, 10)));
        assertAll(attempts.stream().map(attempt -> (Executable) () ->
                assertThrows(ConstraintViolationException.class, attempt)));
    }

    @Test
    void passwordsOverSixteenCannotCreateOrResetAnAccount() {
        assertThrows(ConstraintViolationException.class, () -> users.createUser(new CreateUserCommand(
                "unused", "Arctest899!@#Extra1", "Staff", null, Set.of("OPERATIONS_STAFF"))));
        assertThrows(com.transit.arctransit.common.exception.CommandValidationException.class,
                () -> users.resetPassword("unused", "Arctest899!@#Extra1"));
    }
}
