package com.transit.arctransit;

import com.transit.arctransit.fleet.*;
import com.transit.arctransit.common.exception.CommandValidationException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ARC_TEST_POSTGRES", matches = "true")
class FleetStateIntegrationTest {
    @Autowired FleetManagementService fleet;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "fleet-state-test", null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void capacityBoundariesAreEnforcedByServiceForCreationAndEditing() {
        FleetUnitView unit = create(80);
        assertEquals((short) 80, unit.capacity());
        assertEquals((short) 1, create(1).capacity());
        for (int capacity : new int[]{0, -1, 81, 600}) {
            String key = key();
            assertThrows(ConstraintViolationException.class, () -> fleet.createUnit(
                    new CreateFleetUnitCommand(key, key, (short) capacity)));
            assertThrows(ConstraintViolationException.class, () -> fleet.updateUnit(
                    new UpdateFleetUnitCommand(unit.id(), unit.unitNumber(), unit.plateNumber(), (short) capacity)));
        }
        assertEquals((short) 80, fleet.getUnit(unit.id()).capacity());
    }

    @Test
    void combinedSaveKeepsAdministrativeAndReportedStateSeparateAndRecordsBothChanges() {
        FleetUnitView unit = create(40);
        fleet.updateState(new UpdateFleetStateCommand(unit.id(), "ACTIVE", "AVAILABLE", "Reported by terminal staff."));
        FleetUnitView saved = fleet.getUnit(unit.id());
        assertEquals("ACTIVE", saved.administrativeStatus());
        assertEquals("AVAILABLE", saved.availability());
        assertEquals(2L, count("vehicle_availability_history", "fleet_unit_id", unit.id()));
        assertEquals(1L, audit("FLEET_ADMINISTRATIVE_STATUS_CHANGED", unit.id()));
        assertEquals(1L, audit("FLEET_AVAILABILITY_CHANGED", unit.id()));
        assertEquals("UNVERIFIED", jdbc.queryForObject("""
                SELECT verification_status FROM arc.vehicle_availability_history
                WHERE fleet_unit_id=? ORDER BY id DESC LIMIT 1
                """, String.class, unit.id()));
    }

    @Test
    void administrativeOnlySaveDoesNotInventAnAvailabilityReportOrRequireItsReason() {
        FleetUnitView unit = create(40);
        fleet.updateState(new UpdateFleetStateCommand(unit.id(), "ACTIVE", "UNKNOWN", null));
        assertEquals("ACTIVE", fleet.getUnit(unit.id()).administrativeStatus());
        assertEquals("UNKNOWN", fleet.getUnit(unit.id()).availability());
        assertEquals(1L, count("vehicle_availability_history", "fleet_unit_id", unit.id()));
        assertEquals(0L, audit("FLEET_AVAILABILITY_CHANGED", unit.id()));
        fleet.updateState(new UpdateFleetStateCommand(unit.id(), "ACTIVE", "UNKNOWN", null));
        assertEquals(1L, audit("FLEET_ADMINISTRATIVE_STATUS_CHANGED", unit.id()));
    }

    @Test
    void invalidReasonOrStateDoesNotPartiallySaveTheAdministrativeChange() {
        FleetUnitView unit = create(40);
        assertThrows(CommandValidationException.class, () -> fleet.updateState(
                new UpdateFleetStateCommand(unit.id(), "ACTIVE", "AVAILABLE", "  ")));
        assertThrows(CommandValidationException.class, () -> fleet.updateState(
                new UpdateFleetStateCommand(unit.id(), "ACTIVE", "FLYING", "Invalid choice")));
        assertEquals("INACTIVE", fleet.getUnit(unit.id()).administrativeStatus());
        assertEquals("UNKNOWN", fleet.getUnit(unit.id()).availability());
        assertEquals(0L, audit("FLEET_ADMINISTRATIVE_STATUS_CHANGED", unit.id()));
        assertEquals(1L, count("vehicle_availability_history", "fleet_unit_id", unit.id()));
        fleet.archiveUnit(unit.id());
        assertThrows(CommandValidationException.class, () -> fleet.updateState(
                new UpdateFleetStateCommand(unit.id(), "ACTIVE", "AVAILABLE", "Archived bus")));
    }

    @Test
    void anAuditFailureRollsBackBothStatesAndTheNewHistory() {
        FleetUnitView unit = create(40);
        jdbc.execute("""
                CREATE FUNCTION arc.test_reject_combined_state_audit() RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.action_name = 'FLEET_AVAILABILITY_CHANGED' THEN
                        RAISE EXCEPTION 'test availability audit unavailable' USING ERRCODE = '23514';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """);
        jdbc.execute("CREATE TRIGGER test_reject_combined_state_audit BEFORE INSERT ON arc.audit_logs "
                + "FOR EACH ROW EXECUTE FUNCTION arc.test_reject_combined_state_audit()");
        try {
            assertThrows(DataIntegrityViolationException.class, () -> fleet.updateState(
                    new UpdateFleetStateCommand(unit.id(), "ACTIVE", "AVAILABLE", "Reported ready")));
            assertEquals("INACTIVE", fleet.getUnit(unit.id()).administrativeStatus());
            assertEquals("UNKNOWN", fleet.getUnit(unit.id()).availability());
            assertEquals(1L, count("vehicle_availability_history", "fleet_unit_id", unit.id()));
            assertEquals(0L, audit("FLEET_ADMINISTRATIVE_STATUS_CHANGED", unit.id()));
            assertEquals(0L, audit("FLEET_AVAILABILITY_CHANGED", unit.id()));
        } finally {
            jdbc.execute("DROP TRIGGER test_reject_combined_state_audit ON arc.audit_logs");
            jdbc.execute("DROP FUNCTION arc.test_reject_combined_state_audit()");
        }
    }

    @Test
    void directSqlCannotInsertOrChangeCapacityAboveEighty() {
        String key = key();
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO arc.fleet_units(unit_number,plate_number,capacity,created_at,updated_at,
                    administrative_status,availability,availability_effective_at)
                VALUES (?,?,600,now(),now(),'INACTIVE','UNKNOWN',now())
                """, key, key));
        FleetUnitView unit = create(40);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE arc.fleet_units SET capacity=81 WHERE id=?", unit.id()));
        assertEquals((short) 40, fleet.getUnit(unit.id()).capacity());
    }

    private FleetUnitView create(int capacity) {
        String key = key();
        return fleet.createUnit(new CreateFleetUnitCommand(key, key, (short) capacity));
    }

    private static String key() {
        return "CAP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
    }

    private long count(String table, String column, Long id) {
        return jdbc.queryForObject("SELECT count(*) FROM arc." + table + " WHERE " + column + "=?", Long.class, id);
    }

    private long audit(String action, Long id) {
        return jdbc.queryForObject("SELECT count(*) FROM arc.audit_logs WHERE action_name=? AND entity_name='FleetUnit' AND entity_id=?",
                Long.class, action, id);
    }
}
