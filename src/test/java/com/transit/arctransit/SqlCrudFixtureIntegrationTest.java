package com.transit.arctransit;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "ARC_TEST_POSTGRES", matches = "true")
class SqlCrudFixtureIntegrationTest {

    private static String databaseUrl;
    private static String username;
    private static String password;

    @BeforeAll
    static void migrateAndSeedDedicatedDatabase() throws Exception {
        String baseUrl = System.getenv("SPRING_DATASOURCE_URL");
        username = System.getenv("SPRING_DATASOURCE_USERNAME");
        password = System.getenv("POSTGRES_PASSWORD");
        String database = "arc_transit_crud_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(baseUrl, username, password);
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + database);
        }
        databaseUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1) + database;
        Flyway.configure().dataSource(databaseUrl, username, password)
                .schemas("arc").defaultSchema("arc").load().migrate();
        executeFile(databaseUrl, "01_seed_resources.sql");
    }

    @Test
    void fixturesMatchRegistrationDefaultsAndRetainExplicitSqlProvenance() throws Exception {
        try (var connection = connect()) {
            assertEquals(2, count(connection, """
                    SELECT count(*) FROM arc.fleet_units
                    WHERE administrative_status = 'INACTIVE' AND operational_status = 'INACTIVE'
                      AND availability = 'UNKNOWN' AND vehicle_type = 'BUS' AND capacity > 0
                      AND archived_at IS NULL AND version = 0
                    """));
            assertEquals(2, count(connection, """
                    SELECT count(*) FROM arc.drivers
                    WHERE employment_status = 'ACTIVE' AND availability = 'UNKNOWN'
                      AND license_type = 'PROFESSIONAL' AND license_expiry_date > CURRENT_DATE
                      AND archived_at IS NULL AND version = 0
                    """));
            for (String table : new String[]{"vehicle_availability_history", "driver_availability_history"}) {
                assertEquals(2, count(connection, "SELECT count(*) FROM arc." + table + """
                         WHERE previous_availability IS NULL AND availability = 'UNKNOWN'
                           AND recorded_by = 'SQL_TEST_FIXTURE' AND source_type = 'REGISTRATION'
                           AND verification_status = 'UNVERIFIED' AND effective_at <= recorded_at
                        """));
            }
            assertEquals(4, count(connection, """
                    SELECT count(*) FROM arc.audit_logs WHERE action_name = 'SQL_TEST_FIXTURE_CREATED'
                      AND performed_by = 'SQL_TEST_FIXTURE' AND outcome = 'SUCCESS'
                    """));
            assertEquals(0, count(connection, "SELECT count(*) FROM arc.audit_logs WHERE action_name = 'FLEET_UNIT_REGISTERED'"));
        }
    }

    @Test
    void expectedConstraintFailuresAndApplicationOnlyBoundaryProbeLeaveNoChanges() throws Exception {
        executeFile(databaseUrl, "02_constraint_probes.sql");
        try (var connection = connect()) {
            assertEquals(1, count(connection, "SELECT count(*) FROM arc.drivers WHERE employee_number = 'SQLQA-DRV-01' AND first_name = 'Test'"));
            assertEquals(1, count(connection, "SELECT count(*) FROM arc.fleet_units WHERE unit_number = 'SQLQA-BUS-01' AND capacity = 40 AND plate_number = 'SQLQA-PLATE-01'"));
            assertEquals(2, count(connection, "SELECT count(*) FROM arc.vehicle_availability_history"));
            assertEquals(4, count(connection, "SELECT count(*) FROM arc.audit_logs"));
        }
        executeFile(databaseUrl, "03_inspect_crud.sql");
    }

    @Test
    void rerunningSeedRejectsDuplicatesWithoutPartialInsertions() throws Exception {
        SQLException exception = assertThrows(SQLException.class, () -> executeFile(databaseUrl, "01_seed_resources.sql"));
        assertTrue(exception.getMessage().contains("Fixtures already exist"));
        try (var connection = connect()) {
            assertEquals(2, count(connection, "SELECT count(*) FROM arc.fleet_units"));
            assertEquals(2, count(connection, "SELECT count(*) FROM arc.drivers"));
            assertEquals(4, count(connection, "SELECT count(*) FROM arc.audit_logs"));
        }
    }

    @Test
    void seedAndProbesRejectTheOrdinaryApplicationDatabaseName() {
        for (String file : new String[]{"01_seed_resources.sql", "02_constraint_probes.sql"}) {
            SQLException exception = assertThrows(SQLException.class,
                    () -> executeFile(System.getenv("SPRING_DATASOURCE_URL"), file));
            assertTrue(exception.getMessage().contains("Use a dedicated arc_transit_crud_test database"));
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(databaseUrl, username, password);
    }

    private static int count(Connection connection, String query) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(query)) {
            assertTrue(rows.next());
            return rows.getInt(1);
        }
    }

    private static void executeFile(String url, String name) throws Exception {
        String sql = Files.readString(Path.of("tests", "sql", name));
        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
