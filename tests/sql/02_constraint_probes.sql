-- Run immediately after 01_seed_resources.sql, before editing fixtures through the UI.
-- Expected failures are asserted inside subtransactions. All changes are rolled back.
BEGIN;

DO $$
BEGIN
    IF current_database() !~ '^arc_transit_crud_test(_[a-z0-9]+)?$' THEN
        RAISE EXCEPTION 'Use a dedicated arc_transit_crud_test database. Current database: %', current_database();
    END IF;
    IF (SELECT count(*) FROM arc.fleet_units WHERE unit_number IN ('SQLQA-BUS-01', 'SQLQA-BUS-02')) <> 2
        OR (SELECT count(*) FROM arc.drivers WHERE employee_number = 'SQLQA-DRV-01') <> 1 THEN
        RAISE EXCEPTION 'Run the seed file first and probe before editing its fixtures.';
    END IF;
END;
$$;

CREATE FUNCTION pg_temp.expect_error(command TEXT, expected_state TEXT, expected_constraint TEXT DEFAULT NULL)
RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    actual_state TEXT;
    actual_constraint TEXT;
BEGIN
    BEGIN
        EXECUTE command;
    EXCEPTION WHEN OTHERS THEN
        GET STACKED DIAGNOSTICS actual_state = RETURNED_SQLSTATE, actual_constraint = CONSTRAINT_NAME;
        IF actual_state = expected_state
            AND (expected_constraint IS NULL OR actual_constraint = expected_constraint) THEN
            RAISE NOTICE 'PASS: SQLSTATE %, constraint %', actual_state, actual_constraint;
            RETURN;
        END IF;
        RAISE;
    END;
    RAISE EXCEPTION 'Expected SQLSTATE % but command succeeded: %', expected_state, command;
END;
$$;

SELECT pg_temp.expect_error(
    $$UPDATE arc.fleet_units SET capacity = 0 WHERE unit_number = 'SQLQA-BUS-01'$$,
    '23514', 'ck_fleet_capacity_range');
SELECT pg_temp.expect_error(
    $$UPDATE arc.fleet_units SET capacity = 600 WHERE unit_number = 'SQLQA-BUS-01'$$,
    '23514', 'ck_fleet_capacity_range');
SELECT pg_temp.expect_error(
    $$UPDATE arc.fleet_units SET plate_number = 'SQLQA-PLATE-02' WHERE unit_number = 'SQLQA-BUS-01'$$,
    '23505', 'ux_fleet_plate_number');
SELECT pg_temp.expect_error(
    $$UPDATE arc.fleet_units SET unit_number = ' lowercase ' WHERE unit_number = 'SQLQA-BUS-01'$$,
    '23514', 'ck_fleet_unit_number_normalized');
SELECT pg_temp.expect_error(
    $$UPDATE arc.fleet_units SET unit_number = repeat('X', 31) WHERE unit_number = 'SQLQA-BUS-01'$$,
    '22001');
SELECT pg_temp.expect_error(
    $$UPDATE arc.fleet_units SET availability = 'FLYING' WHERE unit_number = 'SQLQA-BUS-01'$$,
    '23514', 'ck_fleet_availability');
SELECT pg_temp.expect_error(
    $$INSERT INTO arc.vehicle_availability_history
        (fleet_unit_id, availability, effective_at, recorded_at, recorded_by, source_type, verification_status, reason)
      VALUES (-1, 'UNKNOWN', now(), now(), 'SQL_TEST_FIXTURE', 'REGISTRATION', 'UNVERIFIED', 'Negative probe')$$,
    '23503', 'vehicle_history_existing_fleet_unit');
SELECT pg_temp.expect_error(
    $$UPDATE arc.vehicle_availability_history SET reason = 'Changed history'
      WHERE fleet_unit_id = (SELECT id FROM arc.fleet_units WHERE unit_number = 'SQLQA-BUS-01')$$,
    '23514');
SELECT pg_temp.expect_error(
    $$DELETE FROM arc.audit_logs WHERE action_name = 'SQL_TEST_FIXTURE_CREATED'$$,
    'P0001');

-- A deliberate application-only boundary probe: DB varchar(80) accepts 51 characters,
-- while CreateDriverCommand and UpdateDriverCommand reject names above 50.
DO $$
BEGIN
    UPDATE arc.drivers SET first_name = repeat('R', 51) WHERE employee_number = 'SQLQA-DRV-01';
    IF NOT EXISTS (SELECT 1 FROM arc.drivers WHERE employee_number = 'SQLQA-DRV-01' AND length(first_name) = 51) THEN
        RAISE EXCEPTION 'Application-only boundary probe did not run.';
    END IF;
    RAISE NOTICE 'PASS: DB accepts a 51-character first name; application validation must reject it. Rolling back.';
END;
$$;

ROLLBACK;
