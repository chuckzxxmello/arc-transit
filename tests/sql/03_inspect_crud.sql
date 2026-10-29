-- Read-only evidence. Run before/after each UI operation and retain the output.
BEGIN TRANSACTION READ ONLY;

SELECT current_database() AS database, current_user AS database_role,
    current_setting('TimeZone') AS session_timezone;
SELECT version, description, success FROM arc.flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

SELECT id, unit_number, plate_number, capacity, operational_status, administrative_status,
    availability, availability_effective_at, archived_at, version, created_at, updated_at
FROM arc.fleet_units WHERE unit_number LIKE 'SQLQA-%' ORDER BY id;
SELECT id, employee_number, first_name, last_name, license_type, license_expiry_date,
    employment_status, availability, archived_at, version, updated_at
FROM arc.drivers WHERE employee_number LIKE 'SQLQA-%' ORDER BY id;

-- Fixture audit IDs still identify history after a bus master row is permanently deleted.
SELECT history.* FROM arc.vehicle_availability_history history
WHERE history.fleet_unit_id IN (
    SELECT entity_id FROM arc.audit_logs
    WHERE action_name = 'SQL_TEST_FIXTURE_CREATED' AND entity_name = 'FleetUnit')
ORDER BY history.fleet_unit_id, history.recorded_at, history.id;
SELECT history.* FROM arc.driver_availability_history history
WHERE history.driver_id IN (
    SELECT entity_id FROM arc.audit_logs
    WHERE action_name = 'SQL_TEST_FIXTURE_CREATED' AND entity_name = 'Driver')
ORDER BY history.driver_id, history.recorded_at, history.id;
SELECT entity_name, entity_id, action_name, performed_by, timestamp, outcome, details
FROM arc.audit_logs audit
WHERE EXISTS (
    SELECT 1 FROM arc.audit_logs fixture
    WHERE fixture.action_name = 'SQL_TEST_FIXTURE_CREATED'
      AND fixture.entity_name = audit.entity_name AND fixture.entity_id = audit.entity_id)
ORDER BY timestamp, id;

ROLLBACK;
