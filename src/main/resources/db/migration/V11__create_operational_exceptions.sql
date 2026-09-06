-- V11__create_operational_exceptions.sql
--
-- Creates the arc.operational_exceptions table for the reconciliation module.
-- Stores detected contradictions, stale-state violations, scheduling conflicts,
-- and missing data issues identified by the reconciliation engine.
--
-- Design decisions:
--   1. exception_type classifies the nature of the operational anomaly.
--   2. exception_status tracks the acknowledgement/resolution workflow.
--   3. Partial unique index prevents duplicate open exceptions for the same entity+type.
--   4. BRIN index on detected_at for efficient time-range scans on append-heavy data.
--
-- Source: PostgreSQL 17 Partial Indexes — https://www.postgresql.org/docs/current/indexes-partial.html
-- Source: PostgreSQL 17 BRIN Indexes — https://www.postgresql.org/docs/current/brin-intro.html

CREATE TABLE arc.operational_exceptions (
    id                   BIGSERIAL PRIMARY KEY,
    exception_type       VARCHAR(30) NOT NULL
        CONSTRAINT ck_exception_type CHECK (exception_type IN ('CONTRADICTION', 'STALE_STATE', 'MISSING_DATA', 'SCHEDULING_CONFLICT')),
    severity             VARCHAR(20) NOT NULL
        CONSTRAINT ck_exception_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    affected_entity_type VARCHAR(30) NOT NULL,
    affected_entity_id   BIGINT NOT NULL,
    description          TEXT NOT NULL,
    exception_status     VARCHAR(30) NOT NULL DEFAULT 'OPEN'
        CONSTRAINT ck_exception_status CHECK (exception_status IN ('OPEN', 'ACKNOWLEDGED', 'INVESTIGATING', 'RESOLVED', 'DISMISSED')),
    detected_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    acknowledged_by      VARCHAR(100),
    acknowledged_at      TIMESTAMP WITH TIME ZONE,
    resolved_by          VARCHAR(100),
    resolved_at          TIMESTAMP WITH TIME ZONE,
    resolution_notes     TEXT,
    version              BIGINT NOT NULL DEFAULT 0
);

-- Partial unique index: prevent duplicate open exceptions for the same entity+type
CREATE UNIQUE INDEX ux_exception_open_entity
    ON arc.operational_exceptions (exception_type, affected_entity_type, affected_entity_id)
    WHERE exception_status IN ('OPEN', 'ACKNOWLEDGED', 'INVESTIGATING');

-- BRIN index for time-range queries on append-heavy table
CREATE INDEX idx_exceptions_detected_at_brin
    ON arc.operational_exceptions USING brin (detected_at);
