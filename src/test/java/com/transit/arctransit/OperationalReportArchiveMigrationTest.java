package com.transit.arctransit;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "ARC_TEST_POSTGRES", matches = "true")
class OperationalReportArchiveMigrationTest {
    @Test
    void v26PreservesLegacyReportsLinksAndHistoryWhileAllowingGuardedArchiveDeletion() throws Exception {
        String baseUrl = System.getenv("SPRING_DATASOURCE_URL");
        String username = System.getenv("SPRING_DATASOURCE_USERNAME");
        String password = System.getenv("POSTGRES_PASSWORD");
        String database = "arc_report_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(baseUrl, username, password);
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        String url = baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1) + database;
        Flyway.configure().dataSource(url, username, password).schemas("arc").defaultSchema("arc").target("25").load().migrate();
        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO arc.fleet_units(unit_number,plate_number,capacity,administrative_status,
                        availability,availability_effective_at,created_at,updated_at)
                    VALUES ('ARC-LEGACY','LGC001',40,'ACTIVE','UNKNOWN',now(),now(),now())
                    """);
            statement.execute("""
                    INSERT INTO arc.operational_reports(source_type,reported_by,reported_at,effective_at,
                        entity_type,entity_id,description,verification_status)
                    VALUES ('RADIO','legacy.staff',now(),now(),'FLEET',1,'Legacy linked report','UNVERIFIED'),
                           ('PHONE','legacy.staff',now(),now(),'FLEET',1,'Legacy unused report','UNVERIFIED')
                    """);
            statement.execute("""
                    INSERT INTO arc.report_verification_events(report_id,new_status,recorded_by,recorded_at,reason)
                    VALUES (1,'UNVERIFIED','legacy.staff',now(),'Retain first history'),
                           (2,'UNVERIFIED','legacy.staff',now(),'Retain second history')
                    """);
            statement.execute("""
                    INSERT INTO arc.incidents(title,severity,status,effective_at,reported_at,created_at,updated_at,
                        affected_service_type,affected_entity_id,operational_report_id)
                    VALUES ('Linked legacy incident','LOW','REPORTED',now(),now(),now(),now(),'FLEET',1,1)
                    """);
        }
        var flyway = Flyway.configure().dataSource(url, username, password).schemas("arc").defaultSchema("arc").load();
        flyway.migrate();
        flyway.validate();
        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT count(*),count(archived_at) FROM arc.operational_reports")) {
                assertTrue(rows.next()); assertEquals(2, rows.getInt(1)); assertEquals(0, rows.getInt(2));
            }
            assertThrows(SQLException.class, () -> statement.executeUpdate("DELETE FROM arc.operational_reports WHERE id=1"));
            statement.executeUpdate("UPDATE arc.operational_reports SET archived_at=now() WHERE id=2");
            assertEquals(1, statement.executeUpdate("DELETE FROM arc.operational_reports WHERE id=2"));
            try (var rows = statement.executeQuery("SELECT reason FROM arc.report_verification_events WHERE report_id=2")) {
                assertTrue(rows.next()); assertEquals("Retain second history", rows.getString(1));
            }
            assertThrows(SQLException.class, () -> statement.executeUpdate("DELETE FROM arc.report_verification_events WHERE report_id=2"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO arc.report_verification_events(report_id,new_status,recorded_by,recorded_at,reason)
                    VALUES (2,'UNVERIFIED','probe',now(),'Invalid orphan event')
                    """));
        }
    }
}
