package com.transit.arctransit;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "ARC_TEST_POSTGRES", matches = "true")
class FleetCapacityMigrationIntegrationTest {
    @Test
    void migrationPreservesLegacyCapacityButRejectsNewOversizedValues() throws Exception {
        String base = System.getenv("SPRING_DATASOURCE_URL");
        String username = System.getenv("SPRING_DATASOURCE_USERNAME");
        String password = System.getenv("POSTGRES_PASSWORD");
        String database = "arc_capacity_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(base, username, password);
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        String url = base.substring(0, base.lastIndexOf('/') + 1) + database;
        Flyway.configure().dataSource(url, username, password).schemas("arc").defaultSchema("arc")
                .target("21").load().migrate();
        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO arc.fleet_units(unit_number,plate_number,capacity,created_at,updated_at,
                        administrative_status,availability,availability_effective_at)
                    VALUES ('LEGACY-CAP','LEGACY-CAP',600,now(),now(),'INACTIVE','UNKNOWN',now())
                    """);
            statement.execute("""
                    INSERT INTO arc.drivers(employee_number,first_name,last_name,license_number,license_type,
                        license_expiry_date,contact_number,created_at,updated_at,availability,availability_effective_at)
                    VALUES ('LEGACY-CONTACT','Legacy','Driver','LEGACY-CONTACT','PROFESSIONAL',
                        current_date + 365,'+639691477344',now(),now(),'UNKNOWN',now())
                    """);
        }
        Flyway.configure().dataSource(url, username, password).schemas("arc").defaultSchema("arc").load().migrate();
        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT capacity FROM arc.fleet_units WHERE unit_number='LEGACY-CAP'")) {
                assertTrue(rows.next());
                assertEquals(600, rows.getInt(1));
            }
            assertEquals(1, statement.executeUpdate("UPDATE arc.fleet_units SET administrative_status='ACTIVE' WHERE unit_number='LEGACY-CAP'"));
            SQLException exception = assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "UPDATE arc.fleet_units SET capacity=81 WHERE unit_number='LEGACY-CAP'"));
            assertEquals("23514", exception.getSQLState());
            assertEquals(1, statement.executeUpdate("UPDATE arc.fleet_units SET capacity=80 WHERE unit_number='LEGACY-CAP'"));
            try (var rows = statement.executeQuery("SELECT contact_number FROM arc.drivers WHERE employee_number='LEGACY-CONTACT'")) {
                assertTrue(rows.next());
                assertEquals("+639691477344", rows.getString(1));
            }
            assertEquals(1, statement.executeUpdate("UPDATE arc.drivers SET employment_status='INACTIVE' WHERE employee_number='LEGACY-CONTACT'"));
            assertEquals("23514", assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "UPDATE arc.drivers SET contact_number='123' WHERE employee_number='LEGACY-CONTACT'")).getSQLState());
            assertEquals(1, statement.executeUpdate("UPDATE arc.drivers SET contact_number='09691477344' WHERE employee_number='LEGACY-CONTACT'"));
        }
    }
}
