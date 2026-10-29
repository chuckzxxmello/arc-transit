package com.transit.arctransit;

import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.fleet.CreateFleetUnitCommand;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitView;
import com.transit.arctransit.maintenance.CreateMaintenanceCommand;
import com.transit.arctransit.maintenance.MaintenanceService;
import com.transit.arctransit.reporting.CreateOperationalReportCommand;
import com.transit.arctransit.reporting.OperationalReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ARC_TEST_POSTGRES", matches = "true")
class FleetDeletionIntegrationTest {
    @Autowired FleetManagementService fleet;
    @Autowired MaintenanceService maintenance;
    @Autowired OperationalReportService reports;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "fleet-delete-test", null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void automaticRegistrationHistoryAndAuditDoNotBlockDeletionAndRemainIntact() {
        FleetUnitView unit = createBus();
        assertEquals(1L, historyCount(unit.id()));
        fleet.archiveUnit(unit.id());
        fleet.hardDeleteUnit(unit.id());
        assertEquals(0L, unitCount(unit.id()));
        assertEquals(1L, historyCount(unit.id()));
        assertEquals(1L, deletionAuditCount(unit.id()));
        String details = jdbc.queryForObject("""
                SELECT details FROM arc.audit_logs
                WHERE action_name = 'FLEET_UNIT_DELETED' AND entity_id = ?
                """, String.class, unit.id());
        assertTrue(details.contains(unit.unitNumber()));
        assertTrue(details.contains(unit.plateNumber()));
        assertEquals(3L, jdbc.queryForObject("SELECT count(*) FROM arc.audit_logs WHERE entity_name='FleetUnit' AND entity_id=?",
                Long.class, unit.id()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE arc.vehicle_availability_history SET reason='rewritten' WHERE fleet_unit_id=?", unit.id()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "DELETE FROM arc.vehicle_availability_history WHERE fleet_unit_id=?", unit.id()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO arc.vehicle_availability_history(fleet_unit_id, availability, effective_at, recorded_at,
                    recorded_by, source_type, verification_status, reason)
                VALUES (?, 'UNKNOWN', now(), now(), 'test', 'STAFF_ENTRY', 'UNVERIFIED', 'Invalid late event')
                """, unit.id()));
        assertEquals(1L, historyCount(unit.id()));
    }

    @Test
    void busMustBeArchivedBeforeDeletion() {
        FleetUnitView unit = createBus();
        assertTrue(assertThrows(BusinessConflictException.class, () -> fleet.hardDeleteUnit(unit.id()))
                .getMessage().contains("Archive"));
        assertEquals(1L, unitCount(unit.id()));
        assertEquals(0L, deletionAuditCount(unit.id()));
    }

    @Test
    void realMaintenanceReferenceProducesAnActionableBlocker() {
        FleetUnitView unit = createBus();
        maintenance.requestMaintenance(new CreateMaintenanceCommand(unit.id(), "SCHEDULED", "Inspection", null));
        fleet.archiveUnit(unit.id());
        assertTrue(assertThrows(BusinessConflictException.class, () -> fleet.hardDeleteUnit(unit.id()))
                .getMessage().contains("maintenance records"));
        assertEquals(1L, unitCount(unit.id()));
        assertEquals(0L, deletionAuditCount(unit.id()));
    }

    @Test
    void polymorphicReportLinksAlsoBlockDeletion() {
        FleetUnitView unit = createBus();
        reports.submitReport(new CreateOperationalReportCommand("RADIO", "FLEET", unit.id(),
                Instant.now().minusSeconds(1), "Staff-reported condition"));
        fleet.archiveUnit(unit.id());
        assertTrue(assertThrows(BusinessConflictException.class, () -> fleet.hardDeleteUnit(unit.id()))
                .getMessage().contains("operational reports"));
        assertEquals(1L, unitCount(unit.id()));
        assertEquals(0L, deletionAuditCount(unit.id()));
    }

    @Test
    void failedAuditRollsBackPermanentDeletion() {
        FleetUnitView unit = createBus();
        fleet.archiveUnit(unit.id());
        jdbc.execute("""
                CREATE FUNCTION arc.test_reject_bus_deletion_audit() RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.action_name = 'FLEET_UNIT_DELETED' THEN
                        RAISE EXCEPTION 'test audit unavailable' USING ERRCODE = '23514';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """);
        jdbc.execute("CREATE TRIGGER test_reject_bus_deletion_audit BEFORE INSERT ON arc.audit_logs "
                + "FOR EACH ROW EXECUTE FUNCTION arc.test_reject_bus_deletion_audit()");
        try {
            assertThrows(DataIntegrityViolationException.class, () -> fleet.hardDeleteUnit(unit.id()));
            assertEquals(1L, unitCount(unit.id()));
            assertEquals(1L, historyCount(unit.id()));
            assertEquals(0L, deletionAuditCount(unit.id()));
        } finally {
            jdbc.execute("DROP TRIGGER test_reject_bus_deletion_audit ON arc.audit_logs");
            jdbc.execute("DROP FUNCTION arc.test_reject_bus_deletion_audit()");
        }
    }

    @Test
    void aReportBeingRecordedCannotBecomeOrphanedByConcurrentDeletion() throws Exception {
        FleetUnitView unit = createBus();
        fleet.archiveUnit(unit.id());
        var recorded = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var deleteStarted = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var report = executor.submit(() -> {
                authenticate();
                try {
                    new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        reports.submitReport(new CreateOperationalReportCommand("RADIO", "FLEET", unit.id(),
                                Instant.now().minusSeconds(1), "Concurrent staff report"));
                        recorded.countDown();
                        try {
                            if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
                                throw new IllegalStateException("Report transaction timed out waiting for test release.");
                            }
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(exception);
                        }
                    });
                } finally {
                    clearAuthentication();
                }
            });
            assertTrue(recorded.await(10, java.util.concurrent.TimeUnit.SECONDS));
            var deletion = executor.submit(() -> {
                authenticate();
                try {
                    deleteStarted.countDown();
                    return assertThrows(BusinessConflictException.class, () -> fleet.hardDeleteUnit(unit.id()));
                } finally {
                    clearAuthentication();
                }
            });
            assertTrue(deleteStarted.await(10, java.util.concurrent.TimeUnit.SECONDS));
            assertThrows(java.util.concurrent.TimeoutException.class,
                    () -> deletion.get(200, java.util.concurrent.TimeUnit.MILLISECONDS));
            release.countDown();
            report.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(deletion.get(10, java.util.concurrent.TimeUnit.SECONDS).getMessage().contains("operational reports"));
            assertEquals(1L, unitCount(unit.id()));
            assertEquals(0L, deletionAuditCount(unit.id()));
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS));
        }
    }

    private FleetUnitView createBus() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        return fleet.createUnit(new CreateFleetUnitCommand("DEL-" + suffix, "P-" + suffix, (short) 40));
    }

    private long unitCount(Long id) {
        return jdbc.queryForObject("SELECT count(*) FROM arc.fleet_units WHERE id=?", Long.class, id);
    }

    private long historyCount(Long id) {
        return jdbc.queryForObject("SELECT count(*) FROM arc.vehicle_availability_history WHERE fleet_unit_id=?", Long.class, id);
    }

    private long deletionAuditCount(Long id) {
        return jdbc.queryForObject("SELECT count(*) FROM arc.audit_logs WHERE action_name='FLEET_UNIT_DELETED' AND entity_id=?",
                Long.class, id);
    }
}
