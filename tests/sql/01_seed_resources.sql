-- Run the entire file in a dedicated arc_transit_crud_test database.
-- These are synthetic fixtures, not evidence of an authenticated application action.
BEGIN;

DO $$
BEGIN
    IF current_database() !~ '^arc_transit_crud_test(_[a-z0-9]+)?$' THEN
        RAISE EXCEPTION 'Use a dedicated arc_transit_crud_test database. Current database: %', current_database();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM arc.flyway_schema_history WHERE version = '24' AND success) THEN
        RAISE EXCEPTION 'Start the current application against this test database first; migration V24 is required.';
    END IF;
    IF EXISTS (SELECT 1 FROM arc.fleet_units WHERE unit_number IN ('SQLQA-BUS-01', 'SQLQA-BUS-02'))
        OR EXISTS (SELECT 1 FROM arc.drivers WHERE employee_number IN ('SQLQA-DRV-01', 'SQLQA-DRV-02'))
        OR EXISTS (SELECT 1 FROM arc.audit_logs WHERE action_name = 'SQL_TEST_FIXTURE_CREATED') THEN
        RAISE EXCEPTION 'Fixtures already exist or were previously used. Use their existing records or a fresh test database.';
    END IF;
END;
$$;

WITH inserted AS (
    INSERT INTO arc.fleet_units
        (unit_number, plate_number, vehicle_type, capacity, operational_status,
         administrative_status, availability, availability_effective_at, created_at, updated_at, version)
    VALUES
        ('SQLQA-BUS-01', 'SQLQA-PLATE-01', 'BUS', 40, 'INACTIVE', 'INACTIVE', 'UNKNOWN', now(), now(), now(), 0),
        ('SQLQA-BUS-02', 'SQLQA-PLATE-02', 'BUS', 45, 'INACTIVE', 'INACTIVE', 'UNKNOWN', now(), now(), now(), 0)
    RETURNING id, unit_number, availability, availability_effective_at
), history AS (
    INSERT INTO arc.vehicle_availability_history
        (fleet_unit_id, previous_availability, availability, effective_at, recorded_at,
         recorded_by, source_type, verification_status, reason)
    SELECT id, NULL, availability, availability_effective_at, now(),
        'SQL_TEST_FIXTURE', 'REGISTRATION', 'UNVERIFIED',
        'SQL test fixture. Availability is unknown until reported by staff.'
    FROM inserted
    RETURNING fleet_unit_id
)
INSERT INTO arc.audit_logs
    (action_name, entity_name, entity_id, performed_by, timestamp, details, outcome)
SELECT 'SQL_TEST_FIXTURE_CREATED', 'FleetUnit', inserted.id, 'SQL_TEST_FIXTURE', now(),
    'Direct SQL test fixture ' || inserted.unit_number || '; not an application registration.', 'SUCCESS'
FROM inserted JOIN history ON history.fleet_unit_id = inserted.id;

WITH inserted AS (
    INSERT INTO arc.drivers
        (employee_number, first_name, last_name, license_number, license_type, license_expiry_date,
         contact_number, employment_status, availability, availability_effective_at, created_at, updated_at, version)
    VALUES
        ('SQLQA-DRV-01', 'Test', 'Driver One', 'SQLQA-LICENSE-01', 'PROFESSIONAL', CURRENT_DATE + 365,
         NULL, 'ACTIVE', 'UNKNOWN', now(), now(), now(), 0),
        ('SQLQA-DRV-02', 'Test', 'Driver Two', 'SQLQA-LICENSE-02', 'PROFESSIONAL', CURRENT_DATE + 365,
         NULL, 'ACTIVE', 'UNKNOWN', now(), now(), now(), 0)
    RETURNING id, employee_number, availability, availability_effective_at
), history AS (
    INSERT INTO arc.driver_availability_history
        (driver_id, previous_availability, availability, effective_at, recorded_at,
         recorded_by, source_type, verification_status, reason)
    SELECT id, NULL, availability, availability_effective_at, now(),
        'SQL_TEST_FIXTURE', 'REGISTRATION', 'UNVERIFIED',
        'SQL test fixture. Availability is unknown until reported by staff.'
    FROM inserted
    RETURNING driver_id
)
INSERT INTO arc.audit_logs
    (action_name, entity_name, entity_id, performed_by, timestamp, details, outcome)
SELECT 'SQL_TEST_FIXTURE_CREATED', 'Driver', inserted.id, 'SQL_TEST_FIXTURE', now(),
    'Direct SQL test fixture ' || inserted.employee_number || '; not an application registration.', 'SUCCESS'
FROM inserted JOIN history ON history.driver_id = inserted.id;

COMMIT;

SELECT id, unit_number, plate_number, administrative_status, availability
FROM arc.fleet_units WHERE unit_number LIKE 'SQLQA-%' ORDER BY unit_number;
SELECT id, employee_number, employment_status, availability, license_expiry_date
FROM arc.drivers WHERE employee_number LIKE 'SQLQA-%' ORDER BY employee_number;
