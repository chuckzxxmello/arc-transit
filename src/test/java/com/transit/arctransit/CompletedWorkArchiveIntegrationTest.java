package com.transit.arctransit;

import com.transit.arctransit.fleet.*;
import com.transit.arctransit.incident.*;
import com.transit.arctransit.maintenance.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ARC_TEST_POSTGRES", matches = "true")
class CompletedWorkArchiveIntegrationTest {
    @Autowired FleetManagementService fleet;
    @Autowired MaintenanceService maintenance;
    @Autowired IncidentManagementService incidents;
    @Autowired JdbcTemplate jdbc;
    @Value("${arc.security.admin.username}") String administrator;

    @BeforeEach
    void authenticate() { as("SYSTEM_ADMIN"); }

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test
    void completionArchivesMaintenanceAndRetainsHistoryNotesAndAvailabilityRules() {
        var bus = bus();
        String description = marker();
        var record = maintenance.requestMaintenance(new CreateMaintenanceCommand(bus.id(), "CORRECTIVE", description, null));
        maintenance.approveMaintenance(record.id());
        maintenance.startMaintenance(record.id());
        as("OPERATIONS_STAFF");
        maintenance.completeMaintenance(record.id(), "Brake repair completed; staff must confirm availability.");
        assertFalse(maintenance.searchRecords(PageRequest.of(0, 1000)).stream().anyMatch(row -> row.id().equals(record.id())));
        assertEquals("UNKNOWN", fleet.getUnit(bus.id()).availability());
        assertThrows(AccessDeniedException.class, () -> maintenance.searchArchivedRecords(description, PageRequest.of(0, 20)));
        as("SYSTEM_ADMIN");
        var archived = maintenance.searchArchivedRecords(description, PageRequest.of(0, 20));
        assertEquals(1, archived.getTotalElements());
        assertEquals(record.id(), archived.getContent().getFirst().id());
        assertTrue(archived.getContent().getFirst().technicianNotes().contains("Brake repair"));
        assertEquals(4, maintenance.history(record.id(), PageRequest.of(0, 20)).getTotalElements());
        assertNotNull(jdbc.queryForObject("SELECT archived_at FROM arc.maintenance_records WHERE id=?", Object.class, record.id()));
        assertEquals(1L, audit("MAINTENANCE_ARCHIVED", record.id()));
        assertThrows(IllegalStateException.class, () -> maintenance.completeMaintenance(record.id(), "Repeated completion"));
        assertEquals(1L, audit("MAINTENANCE_ARCHIVED", record.id()));
    }

    @Test
    void incidentsStayVisibleUntilExplicitClosureThenMoveToSearchableArchive() {
        String title = marker();
        var incident = incidents.reportIncident(new CreateIncidentCommand(title, "Reported door issue", "MEDIUM", null, null));
        incidents.acknowledgeIncident(incident.id());
        incidents.startInvestigation(incident.id());
        incidents.resolveIncident(incident.id(), "Door inspected and repaired.");
        assertTrue(incidents.searchIncidents(PageRequest.of(0, 1000)).stream().anyMatch(row -> row.id().equals(incident.id())));
        incidents.closeIncident(incident.id());
        assertFalse(incidents.searchIncidents(PageRequest.of(0, 1000)).stream().anyMatch(row -> row.id().equals(incident.id())));
        var archived = incidents.searchArchivedIncidents(title, PageRequest.of(0, 20));
        assertEquals(1, archived.getTotalElements());
        assertEquals("CLOSED", archived.getContent().getFirst().status());
        assertEquals("Door inspected and repaired.", archived.getContent().getFirst().resolutionNotes());
        assertEquals(5, incidents.getHistory(incident.id()).size());
        assertNotNull(jdbc.queryForObject("SELECT archived_at FROM arc.incidents WHERE id=?", Object.class, incident.id()));
        assertEquals(1L, audit("INCIDENT_ARCHIVED", incident.id()));
        as("OPERATIONS_STAFF");
        assertThrows(AccessDeniedException.class, () -> incidents.searchArchivedIncidents(title, PageRequest.of(0, 20)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"maintenance", "incident"})
    void archiveAuditFailureRollsBackLifecycleTimestampAndHistory(String module) {
        Long id;
        if (module.equals("maintenance")) {
            id = maintenance.requestMaintenance(new CreateMaintenanceCommand(bus().id(), "CORRECTIVE", marker(), null)).id();
            maintenance.approveMaintenance(id);
            maintenance.startMaintenance(id);
        } else {
            id = incidents.reportIncident(new CreateIncidentCommand(marker(), null, "LOW", null, null)).id();
            incidents.acknowledgeIncident(id);
            incidents.startInvestigation(id);
            incidents.resolveIncident(id, "Resolved pending closure.");
        }
        jdbc.execute("""
                CREATE FUNCTION arc.reject_completed_archive_audit() RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.action_name IN ('MAINTENANCE_ARCHIVED','INCIDENT_ARCHIVED') THEN
                        RAISE EXCEPTION 'Test archive audit unavailable' USING ERRCODE='23514';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """);
        jdbc.execute("CREATE TRIGGER reject_completed_archive_audit BEFORE INSERT ON arc.audit_logs "
                + "FOR EACH ROW EXECUTE FUNCTION arc.reject_completed_archive_audit()");
        try {
            if (module.equals("maintenance")) {
                assertThrows(RuntimeException.class, () -> maintenance.completeMaintenance(id, "Completed repair."));
                assertEquals("IN_PROGRESS", maintenance.getRecord(id).maintenanceStatus());
                assertEquals(3, maintenance.history(id, PageRequest.of(0, 20)).getTotalElements());
                assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM arc.maintenance_records WHERE id=? AND archived_at IS NOT NULL", Long.class, id));
                assertEquals(0L, audit("MAINTENANCE_COMPLETED", id));
            } else {
                assertThrows(RuntimeException.class, () -> incidents.closeIncident(id));
                assertEquals(4, incidents.getHistory(id).size());
                assertEquals("RESOLVED", jdbc.queryForObject("SELECT status FROM arc.incidents WHERE id=?", String.class, id));
                assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM arc.incidents WHERE id=? AND archived_at IS NOT NULL", Long.class, id));
                assertEquals(0L, audit("INCIDENT_CLOSED", id));
            }
        } finally {
            jdbc.execute("DROP TRIGGER reject_completed_archive_audit ON arc.audit_logs");
            jdbc.execute("DROP FUNCTION arc.reject_completed_archive_audit()");
        }
    }

    private FleetUnitView bus() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return fleet.createUnit(new CreateFleetUnitCommand("ARC-" + suffix, "QA" + suffix, (short) 40));
    }

    private long audit(String action, Long id) {
        return jdbc.queryForObject("SELECT count(*) FROM arc.audit_logs WHERE action_name=? AND entity_id=?", Long.class, action, id);
    }

    private void as(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(administrator, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private static String marker() { return "Completed work " + UUID.randomUUID().toString().substring(0, 8); }
}
