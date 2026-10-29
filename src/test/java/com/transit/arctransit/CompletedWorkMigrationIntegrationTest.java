package com.transit.arctransit;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "ARC_TEST_POSTGRES", matches = "true")
class CompletedWorkMigrationIntegrationTest {
    @Test
    void v25ArchivesOnlyFinishedLegacyWorkAndPreservesItsNotes() throws Exception {
        String baseUrl = System.getenv("SPRING_DATASOURCE_URL");
        String username = System.getenv("SPRING_DATASOURCE_USERNAME");
        String password = System.getenv("POSTGRES_PASSWORD");
        String database = "arc_completed_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(baseUrl, username, password);
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        String url = baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1) + database;
        Flyway.configure().dataSource(url, username, password).schemas("arc").defaultSchema("arc").target("24").load().migrate();
        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO arc.fleet_units(unit_number,plate_number,capacity,administrative_status,
                        availability,availability_effective_at,created_at,updated_at)
                    VALUES ('ARC-LEGACY','LGC001',40,'ACTIVE','UNKNOWN',now(),now(),now())
                    """);
            statement.execute("""
                    INSERT INTO arc.maintenance_records(fleet_unit_id,maintenance_type,maintenance_status,
                        description,technician_notes,completed_at,created_at,updated_at)
                    VALUES (1,'CORRECTIVE','COMPLETED','Legacy repair','Retain technician notes',now(),now(),now()),
                           (1,'SCHEDULED','REQUESTED','Pending work',NULL,NULL,now(),now()),
                           (1,'SCHEDULED','CANCELLED','Cancelled work',NULL,NULL,now(),now())
                    """);
            statement.execute("""
                    INSERT INTO arc.incidents(title,severity,status,resolution_notes,effective_at,reported_at,created_at,updated_at)
                    VALUES ('Closed legacy issue','LOW','CLOSED','Retain resolution notes',now(),now(),now(),now()),
                           ('Resolved awaiting closure','LOW','RESOLVED','Pending closure',now(),now(),now(),now()),
                           ('Open issue','LOW','REPORTED',NULL,now(),now(),now(),now())
                    """);
        }
        var flyway = Flyway.configure().dataSource(url, username, password).schemas("arc").defaultSchema("arc").load();
        flyway.migrate();
        flyway.validate();
        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT count(*),count(archived_at) FROM arc.maintenance_records")) {
                assertTrue(rows.next()); assertEquals(3, rows.getInt(1)); assertEquals(1, rows.getInt(2));
            }
            try (var rows = statement.executeQuery("SELECT technician_notes FROM arc.maintenance_records WHERE archived_at IS NOT NULL")) {
                assertTrue(rows.next()); assertEquals("Retain technician notes", rows.getString(1));
            }
            try (var rows = statement.executeQuery("SELECT count(*),count(archived_at) FROM arc.incidents")) {
                assertTrue(rows.next()); assertEquals(3, rows.getInt(1)); assertEquals(1, rows.getInt(2));
            }
            try (var rows = statement.executeQuery("SELECT status,resolution_notes FROM arc.incidents WHERE archived_at IS NOT NULL")) {
                assertTrue(rows.next()); assertEquals("CLOSED", rows.getString(1)); assertEquals("Retain resolution notes", rows.getString(2));
            }
        }
    }
}
