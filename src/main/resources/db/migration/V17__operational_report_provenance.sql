CREATE TABLE arc.operational_reports (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_type VARCHAR(30) NOT NULL CHECK (source_type IN ('STAFF_ENTRY', 'PHONE', 'RADIO')),
    reported_by VARCHAR(100) NOT NULL CHECK (BTRIM(reported_by) <> ''),
    reported_at TIMESTAMPTZ NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    entity_type VARCHAR(30) NOT NULL CHECK (entity_type IN ('FLEET', 'DRIVER', 'ROUTE', 'DISPATCH')),
    entity_id BIGINT NOT NULL CHECK (entity_id > 0),
    description VARCHAR(4000) NOT NULL CHECK (BTRIM(description) <> ''),
    verification_status VARCHAR(30) NOT NULL CHECK (verification_status IN ('UNVERIFIED', 'CONFIRMED', 'DISPUTED', 'REJECTED')),
    verified_by VARCHAR(100),
    verified_at TIMESTAMPTZ,
    verification_reason VARCHAR(2000),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_report_verification_context CHECK (
        (verification_status = 'UNVERIFIED' AND verified_by IS NULL AND verified_at IS NULL)
        OR (verification_status <> 'UNVERIFIED' AND verified_by IS NOT NULL AND verified_at IS NOT NULL
            AND verification_reason IS NOT NULL AND BTRIM(verification_reason) <> '')),
    CONSTRAINT ck_report_effective_time CHECK (effective_at <= reported_at)
);

CREATE INDEX idx_report_review ON arc.operational_reports (verification_status, effective_at);
CREATE INDEX idx_report_entity ON arc.operational_reports (entity_type, entity_id);

CREATE TABLE arc.report_verification_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES arc.operational_reports(id) ON DELETE RESTRICT,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL CHECK (new_status IN ('UNVERIFIED', 'CONFIRMED', 'DISPUTED', 'REJECTED')),
    recorded_by VARCHAR(100) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    reason VARCHAR(2000) NOT NULL CHECK (BTRIM(reason) <> '')
);

CREATE INDEX idx_report_verification_history ON arc.report_verification_events(report_id, recorded_at, id);

ALTER TABLE arc.incidents
    ADD COLUMN effective_at TIMESTAMPTZ,
    ADD COLUMN source_type VARCHAR(30) NOT NULL DEFAULT 'STAFF_ENTRY',
    ADD COLUMN operational_report_id BIGINT REFERENCES arc.operational_reports(id) ON DELETE RESTRICT,
    ADD COLUMN maintenance_record_id BIGINT REFERENCES arc.maintenance_records(id) ON DELETE RESTRICT;

-- Preserve the known recording timestamp without claiming an independently verified occurrence time.
UPDATE arc.incidents SET effective_at = reported_at;
ALTER TABLE arc.incidents ALTER COLUMN effective_at SET NOT NULL;
ALTER TABLE arc.incidents ADD CONSTRAINT ck_incident_report_time CHECK (effective_at <= reported_at);

CREATE TABLE arc.incident_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES arc.incidents(id) ON DELETE RESTRICT,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    recorded_by VARCHAR(100) NOT NULL,
    reason TEXT NOT NULL
);

INSERT INTO arc.incident_events(incident_id, previous_status, new_status, effective_at, recorded_at, recorded_by, reason)
SELECT id, NULL, status, updated_at, CURRENT_TIMESTAMP, 'SYSTEM_MIGRATION',
       'Legacy current-state snapshot; earlier transitions are unavailable'
FROM arc.incidents;

CREATE INDEX idx_incident_history ON arc.incident_events(incident_id, recorded_at, id);
