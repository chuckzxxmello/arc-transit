CREATE TABLE arc.dispatch_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    assignment_id BIGINT NOT NULL REFERENCES arc.dispatch_assignments(id) ON DELETE RESTRICT,
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    actor VARCHAR(100) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    CONSTRAINT ck_dispatch_event_previous CHECK (previous_status IS NULL OR previous_status IN
        ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_dispatch_event_new CHECK (new_status IN
        ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX ix_dispatch_event_history ON arc.dispatch_events(assignment_id, recorded_at, id);

-- No past transitions are invented for legacy rows.