CREATE TABLE arc.maintenance_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    maintenance_record_id BIGINT NOT NULL REFERENCES arc.maintenance_records(id),
    previous_status VARCHAR(30),
    status VARCHAR(30) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    recorded_by VARCHAR(100) NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    CONSTRAINT ck_maintenance_history_status CHECK (status IN ('REQUESTED', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_maintenance_history_previous CHECK (previous_status IS NULL OR previous_status IN ('REQUESTED', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_maintenance_history_context CHECK (BTRIM(recorded_by) <> '' AND BTRIM(reason) <> '')
);

CREATE INDEX idx_maintenance_history_record
    ON arc.maintenance_history (maintenance_record_id, recorded_at DESC, id DESC);

INSERT INTO arc.maintenance_history
    (maintenance_record_id, status, recorded_at, recorded_by, reason)
SELECT id, maintenance_status, updated_at, 'SYSTEM',
    'Legacy current-state snapshot; previous lifecycle transitions were not recorded.'
FROM arc.maintenance_records;