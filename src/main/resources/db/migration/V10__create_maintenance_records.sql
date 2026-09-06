-- V10__create_maintenance_records.sql
--
-- Creates the arc.maintenance_records table for tracking scheduled,
-- corrective, and emergency maintenance work on fleet units.
--
-- Design decisions:
--   1. Foreign key references arc.fleet_units(id) to enforce referential integrity.
--   2. maintenance_type and maintenance_status use CHECK constraints matching Java enum constants.
--   3. Partial index on (fleet_unit_id) WHERE maintenance_status NOT IN ('COMPLETED','CANCELLED')
--      enables efficient lookup of active maintenance blocking a fleet unit.
--
-- Source: PostgreSQL 17 Foreign Keys — https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-FK
-- Source: PostgreSQL 17 Partial Indexes — https://www.postgresql.org/docs/current/indexes-partial.html

CREATE TABLE arc.maintenance_records (
    id                  BIGSERIAL PRIMARY KEY,
    fleet_unit_id       BIGINT NOT NULL
        CONSTRAINT fk_maintenance_fleet_unit REFERENCES arc.fleet_units(id),
    maintenance_type    VARCHAR(30) NOT NULL
        CONSTRAINT ck_maintenance_type CHECK (maintenance_type IN ('SCHEDULED', 'CORRECTIVE', 'EMERGENCY')),
    maintenance_status  VARCHAR(30) NOT NULL
        CONSTRAINT ck_maintenance_status CHECK (maintenance_status IN ('REQUESTED', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    description         TEXT NOT NULL,
    scheduled_date      DATE,
    started_at          TIMESTAMP WITH TIME ZONE,
    completed_at        TIMESTAMP WITH TIME ZONE,
    technician_notes    TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    version             BIGINT NOT NULL DEFAULT 0
);

-- Partial index: efficiently find active (non-terminal) maintenance for a fleet unit
CREATE INDEX idx_maintenance_active_by_unit
    ON arc.maintenance_records (fleet_unit_id)
    WHERE maintenance_status NOT IN ('COMPLETED', 'CANCELLED');
