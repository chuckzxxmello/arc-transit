CREATE TABLE arc.route_schedules (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES arc.routes(id) ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL CHECK (BTRIM(name) <> ''),
    planned_departure TIME NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ux_route_schedule_name_start UNIQUE (route_id, name, effective_from),
    CONSTRAINT ck_route_schedule_interval CHECK (effective_to IS NULL OR effective_to > effective_from)
);

CREATE INDEX ix_route_schedule_lookup ON arc.route_schedules(route_id, active, effective_from);

ALTER TABLE arc.dispatch_assignments ADD COLUMN route_schedule_id BIGINT
    REFERENCES arc.route_schedules(id) ON DELETE RESTRICT;

CREATE FUNCTION arc.protect_route_schedule() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    PERFORM 1 FROM arc.routes WHERE id = NEW.route_id FOR UPDATE;
    IF TG_OP = 'UPDATE' AND (
        NEW.route_id IS DISTINCT FROM OLD.route_id OR
        NEW.name IS DISTINCT FROM OLD.name OR
        NEW.planned_departure IS DISTINCT FROM OLD.planned_departure OR
        NEW.effective_from IS DISTINCT FROM OLD.effective_from OR
        NEW.effective_to IS DISTINCT FROM OLD.effective_to OR
        NEW.created_at IS DISTINCT FROM OLD.created_at
    ) THEN
        RAISE EXCEPTION 'Configured schedule definitions are immutable; deactivate and create a new schedule'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER route_schedule_definition_guard
    BEFORE INSERT OR UPDATE ON arc.route_schedules
    FOR EACH ROW EXECUTE FUNCTION arc.protect_route_schedule();

CREATE FUNCTION arc.validate_dispatch_schedule() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    configured arc.route_schedules%ROWTYPE;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF NEW.route_schedule_id IS DISTINCT FROM OLD.route_schedule_id OR
           NEW.route_version_id IS DISTINCT FROM OLD.route_version_id THEN
            RAISE EXCEPTION 'An existing trip must retain its original route version and schedule'
                USING ERRCODE = '23514';
        END IF;
        IF NEW.route_id IS NOT DISTINCT FROM OLD.route_id
           AND NEW.dispatch_date IS NOT DISTINCT FROM OLD.dispatch_date THEN
            RETURN NEW;
        END IF;
    END IF;
    IF NEW.route_schedule_id IS NOT NULL THEN
        PERFORM 1 FROM arc.routes WHERE id = NEW.route_id FOR UPDATE;
        SELECT * INTO configured FROM arc.route_schedules WHERE id = NEW.route_schedule_id;
        IF configured.id IS NULL OR configured.route_id <> NEW.route_id OR
           NEW.dispatch_date < configured.effective_from OR
           (configured.effective_to IS NOT NULL AND NEW.dispatch_date >= configured.effective_to) OR
           (TG_OP = 'INSERT' AND NOT configured.active) THEN
            RAISE EXCEPTION 'SCHEDULE_NOT_APPLICABLE' USING ERRCODE = '23514';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER dispatch_schedule_guard
    BEFORE INSERT OR UPDATE OF route_schedule_id, route_version_id, route_id, dispatch_date
    ON arc.dispatch_assignments
    FOR EACH ROW EXECUTE FUNCTION arc.validate_dispatch_schedule();