package com.transit.arctransit;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "ARC_TEST_POSTGRES", matches = "true")
class MigrationUpgradeIntegrationTest {

    @Test
    void legacyV11RecordsSurviveForwardMigration() throws Exception {
        String baseUrl = System.getenv("SPRING_DATASOURCE_URL");
        String username = System.getenv("SPRING_DATASOURCE_USERNAME");
        String password = System.getenv("POSTGRES_PASSWORD");
        String database = "arc_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(baseUrl, username, password);
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        String upgradeUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1) + database;
        Flyway.configure().dataSource(upgradeUrl, username, password)
                .schemas("arc").defaultSchema("arc").target("11").load().migrate();
        try (var connection = DriverManager.getConnection(upgradeUrl, username, password);
             var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO arc.fleet_units(unit_number, plate_number, capacity, operational_status, created_at, updated_at)
                    VALUES ('LEGACY-A', 'OLD-A', 40, 'ACTIVE', now(), now()),
                           ('LEGACY-I', 'OLD-I', 40, 'INACTIVE', now(), now()),
                           ('LEGACY-M', 'OLD-M', 40, 'UNDER_MAINTENANCE', now(), now()),
                           ('LEGACY-O', 'OLD-O', 40, 'OUT_OF_SERVICE', now(), now())
                    """);
            statement.execute("""
                    INSERT INTO arc.drivers(employee_number, first_name, last_name, license_number, license_type,
                        license_expiry_date, employment_status, created_at, updated_at)
                    VALUES ('LEGACY-D', 'Legacy', 'Driver', 'OLD-LICENSE', 'PROFESSIONAL',
                        CURRENT_DATE + 365, 'ACTIVE', now(), now())
                    """);
            statement.execute("""
                    INSERT INTO arc.routes(route_code, route_name, estimated_duration_minutes, created_at, updated_at)
                    VALUES ('LEGACY-R', 'Legacy Route', 60, now(), now())
                    """);
            statement.execute("INSERT INTO arc.route_stops(route_id, stop_name, stop_sequence) VALUES (1, 'Legacy Stop', 1)");
            statement.execute("""
                    INSERT INTO arc.dispatch_assignments(fleet_unit_id, driver_id, route_id, dispatch_date,
                        scheduled_departure, created_at, updated_at)
                    VALUES (1, 1, 1, CURRENT_DATE, now(), now(), now())
                    """);
            statement.execute("""
                    INSERT INTO arc.incidents(title, severity, status, reported_at, created_at, updated_at)
                    VALUES ('Legacy Incident', 'MEDIUM', 'REPORTED', now(), now(), now())
                    """);
            statement.execute("""
                    INSERT INTO arc.drivers(employee_number, first_name, last_name, license_number, license_type,
                        license_expiry_date, employment_status, created_at, updated_at)
                    VALUES ('LEGACY-D2', 'Legacy', 'Completed', 'OLD-LICENSE-2', 'PROFESSIONAL',
                        CURRENT_DATE + 365, 'ACTIVE', now(), now())
                    """);
            statement.execute("""
                    INSERT INTO arc.dispatch_assignments(fleet_unit_id, driver_id, route_id, dispatch_date,
                        scheduled_departure, actual_arrival, dispatch_status, created_at, updated_at)
                    VALUES (4, 2, 1, CURRENT_DATE - 1, now() - INTERVAL '1 day',
                        now() - INTERVAL '23 hours', 'COMPLETED', now(), now())
                    """);
            statement.execute("""
                    INSERT INTO arc.dispatch_assignments(fleet_unit_id, driver_id, route_id, dispatch_date,
                        scheduled_departure, scheduled_arrival, created_at, updated_at)
                    VALUES (2, 2, 1, CURRENT_DATE - 2, now() - INTERVAL '2 days',
                        now() - INTERVAL '3 days', now(), now())
                    """);
        }
        var flyway = Flyway.configure().dataSource(upgradeUrl, username, password)
                .schemas("arc").defaultSchema("arc").load();
        flyway.migrate();
        flyway.validate();
        try (var connection = DriverManager.getConnection(upgradeUrl, username, password);
             var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT operational_status, administrative_status, availability FROM arc.fleet_units ORDER BY id")) {
                assertTrue(rows.next());
                assertEquals("ACTIVE", rows.getString(1));
                assertEquals("ACTIVE", rows.getString(2));
                assertEquals("AVAILABLE", rows.getString(3));
                assertTrue(rows.next());
                assertEquals("INACTIVE", rows.getString(2));
                assertTrue(rows.next());
                assertEquals("MAINTENANCE", rows.getString(3));
                assertTrue(rows.next());
                assertEquals("OUT_OF_SERVICE", rows.getString(3));
            }
            try (var rows = statement.executeQuery("""
                    SELECT version.legacy_snapshot, stop.stop_name
                    FROM arc.dispatch_assignments assignment
                    JOIN arc.route_versions version ON version.id = assignment.route_version_id
                    JOIN arc.route_version_stops stop ON stop.route_version_id = version.id
                    WHERE assignment.id = 1
                    """)) {
                assertTrue(rows.next());
                assertTrue(rows.getBoolean(1));
                assertEquals("Legacy Stop", rows.getString(2));
            }
            try (var rows = statement.executeQuery("SELECT count(*) FROM arc.incident_events WHERE incident_id = 1")) {
                assertTrue(rows.next());
                assertEquals(1, rows.getInt(1));
            }
            assertEquals(1, statement.executeUpdate(
                    "UPDATE arc.dispatch_assignments SET dispatch_status = 'CANCELLED' WHERE id = 3"));
            assertEquals(1, statement.executeUpdate("""
                    INSERT INTO arc.dispatch_assignments(fleet_unit_id, driver_id, route_id, dispatch_date,
                        scheduled_departure, scheduled_arrival, route_version_id, created_at, updated_at)
                    SELECT 4, 2, 1, CURRENT_DATE + 1, now() + INTERVAL '1 day',
                        now() + INTERVAL '25 hours', id, now(), now()
                    FROM arc.route_versions WHERE route_id = 1
                    """));
        }
    }
}
