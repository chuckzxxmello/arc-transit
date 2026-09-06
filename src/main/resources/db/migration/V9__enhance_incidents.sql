-- V9__enhance_incidents.sql
--
-- Enhances the existing arc.incidents table with proper workflow columns,
-- enum check constraints, and affected-service tracking.
--
-- Design decisions:
--   1. ALTER TABLE preserves existing data and Flyway checksum chain.
--   2. severity and status use CHECK constraints matching Java enum constants.
--   3. affected_service_type links incidents to specific operational domains.
--
-- Source: PostgreSQL 17 ALTER TABLE — https://www.postgresql.org/docs/current/sql-altertable.html
-- Source: PostgreSQL 17 CHECK Constraints — https://www.postgresql.org/docs/current/ddl-constraints.html

ALTER TABLE arc.incidents
    ADD COLUMN IF NOT EXISTS affected_service_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS affected_entity_id BIGINT,
    ADD COLUMN IF NOT EXISTS reported_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS acknowledged_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS resolution_notes TEXT;

-- Add check constraint for severity values
ALTER TABLE arc.incidents
    DROP CONSTRAINT IF EXISTS ck_incident_severity;
ALTER TABLE arc.incidents
    ADD CONSTRAINT ck_incident_severity
    CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));

-- Add check constraint for status lifecycle values
ALTER TABLE arc.incidents
    DROP CONSTRAINT IF EXISTS ck_incident_status;
ALTER TABLE arc.incidents
    ADD CONSTRAINT ck_incident_status
    CHECK (status IN ('REPORTED', 'ACKNOWLEDGED', 'INVESTIGATING', 'RESOLVED', 'CLOSED'));

-- Add check constraint for affected service type
ALTER TABLE arc.incidents
    DROP CONSTRAINT IF EXISTS ck_incident_affected_service;
ALTER TABLE arc.incidents
    ADD CONSTRAINT ck_incident_affected_service
    CHECK (affected_service_type IS NULL OR affected_service_type IN ('FLEET', 'DRIVER', 'ROUTE', 'DISPATCH'));

-- Index for querying open incidents efficiently
CREATE INDEX IF NOT EXISTS idx_incidents_status ON arc.incidents (status)
    WHERE status NOT IN ('RESOLVED', 'CLOSED');

-- Update existing rows to conform to new constraints
UPDATE arc.incidents SET severity = 'MEDIUM' WHERE severity NOT IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');
UPDATE arc.incidents SET status = 'REPORTED' WHERE status NOT IN ('REPORTED', 'ACKNOWLEDGED', 'INVESTIGATING', 'RESOLVED', 'CLOSED');
