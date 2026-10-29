CREATE TABLE arc.route_versions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES arc.routes(id) ON DELETE RESTRICT,
    route_code VARCHAR(30) NOT NULL,
    route_name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    estimated_duration_minutes INTEGER,
    effective_from DATE NOT NULL,
    effective_to DATE,
    legacy_snapshot BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ux_route_version_start UNIQUE (route_id, effective_from),
    CONSTRAINT ck_route_version_interval CHECK (effective_to IS NULL OR effective_to > effective_from),
    CONSTRAINT ck_route_version_duration CHECK (estimated_duration_minutes IS NULL OR estimated_duration_minutes > 0),
    CONSTRAINT ck_route_version_name CHECK (BTRIM(route_name) <> ''),
    CONSTRAINT ck_route_version_code CHECK (BTRIM(route_code) <> '')
);

CREATE TABLE arc.route_version_stops (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    route_version_id BIGINT NOT NULL REFERENCES arc.route_versions(id) ON DELETE RESTRICT,
    stop_name VARCHAR(150) NOT NULL CHECK (BTRIM(stop_name) <> ''),
    stop_sequence INTEGER NOT NULL CHECK (stop_sequence > 0),
    estimated_arrival_minutes INTEGER CHECK (estimated_arrival_minutes IS NULL OR estimated_arrival_minutes >= 0),
    CONSTRAINT ux_route_version_stop_sequence UNIQUE (route_version_id, stop_sequence)
);

-- Legacy route edits did not retain old definitions. These snapshots preserve only
-- the configuration available at migration time and must remain labelled as such.
INSERT INTO arc.route_versions(route_id, route_code, route_name, description,
    estimated_duration_minutes, effective_from, legacy_snapshot, created_at)
SELECT route.id, route.route_code, route.route_name, route.description,
    route.estimated_duration_minutes,
    LEAST((route.created_at AT TIME ZONE 'Asia/Manila')::DATE,
        COALESCE((SELECT MIN(assignment.dispatch_date) FROM arc.dispatch_assignments assignment
                   WHERE assignment.route_id = route.id),
                 (route.created_at AT TIME ZONE 'Asia/Manila')::DATE)),
    TRUE, CURRENT_TIMESTAMP
FROM arc.routes route;

INSERT INTO arc.route_version_stops(route_version_id, stop_name, stop_sequence, estimated_arrival_minutes)
SELECT version.id, stop.stop_name, stop.stop_sequence, stop.estimated_arrival_minutes
FROM arc.route_stops stop JOIN arc.route_versions version ON version.route_id = stop.route_id;

ALTER TABLE arc.dispatch_assignments ADD COLUMN route_version_id BIGINT;
UPDATE arc.dispatch_assignments assignment SET route_version_id = version.id
FROM arc.route_versions version WHERE version.route_id = assignment.route_id;
ALTER TABLE arc.dispatch_assignments
    ALTER COLUMN route_version_id SET NOT NULL,
    ADD CONSTRAINT fk_dispatch_route_version FOREIGN KEY (route_version_id)
        REFERENCES arc.route_versions(id) ON DELETE RESTRICT;

-- Legacy incomplete or invalid windows remain reviewable. The trigger below
-- validates new plans and edited windows without blocking legacy lifecycle updates.

CREATE INDEX ix_dispatch_fleet_interval ON arc.dispatch_assignments(fleet_unit_id, scheduled_departure)
    WHERE dispatch_status <> 'CANCELLED';
CREATE INDEX ix_dispatch_driver_interval ON arc.dispatch_assignments(driver_id, scheduled_departure)
    WHERE dispatch_status <> 'CANCELLED';
CREATE INDEX ix_dispatch_version_date ON arc.dispatch_assignments(route_version_id, dispatch_date);
CREATE INDEX ix_dispatch_current_date_status ON arc.dispatch_assignments(dispatch_date, dispatch_status)
    WHERE archived_at IS NULL;

CREATE FUNCTION arc.validate_route_version_interval() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    PERFORM 1 FROM arc.routes WHERE id = NEW.route_id FOR UPDATE;
    IF TG_OP = 'UPDATE' AND (
        NEW.route_id IS DISTINCT FROM OLD.route_id OR
        NEW.route_code IS DISTINCT FROM OLD.route_code OR
        NEW.route_name IS DISTINCT FROM OLD.route_name OR
        NEW.description IS DISTINCT FROM OLD.description OR
        NEW.estimated_duration_minutes IS DISTINCT FROM OLD.estimated_duration_minutes OR
        NEW.effective_from IS DISTINCT FROM OLD.effective_from OR
        NEW.legacy_snapshot IS DISTINCT FROM OLD.legacy_snapshot
    ) THEN
        RAISE EXCEPTION 'Published route definitions are immutable' USING ERRCODE = '23514';
    END IF;
    IF EXISTS (SELECT 1 FROM arc.route_versions version
        WHERE version.route_id = NEW.route_id AND version.id <> NEW.id
          AND version.effective_from < COALESCE(NEW.effective_to, 'infinity'::DATE)
          AND NEW.effective_from < COALESCE(version.effective_to, 'infinity'::DATE)) THEN
        RAISE EXCEPTION 'Route version effective intervals overlap' USING ERRCODE = '23514';
    END IF;
    IF TG_OP = 'UPDATE' AND NEW.effective_to IS NOT NULL AND EXISTS (
        SELECT 1 FROM arc.dispatch_assignments assignment
        WHERE assignment.route_version_id = NEW.id AND assignment.dispatch_date >= NEW.effective_to
    ) THEN
        RAISE EXCEPTION 'Route version interval would invalidate existing trip history' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER route_version_interval_guard
    BEFORE INSERT OR UPDATE ON arc.route_versions
    FOR EACH ROW EXECUTE FUNCTION arc.validate_route_version_interval();

CREATE FUNCTION arc.validate_dispatch_assignment() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    definition arc.route_versions%ROWTYPE;
    check_overlap BOOLEAN;
BEGIN
    check_overlap := TG_OP = 'INSERT';
    IF TG_OP = 'UPDATE' THEN
        check_overlap := NEW.fleet_unit_id IS DISTINCT FROM OLD.fleet_unit_id
            OR NEW.driver_id IS DISTINCT FROM OLD.driver_id
            OR NEW.scheduled_departure IS DISTINCT FROM OLD.scheduled_departure
            OR NEW.scheduled_arrival IS DISTINCT FROM OLD.scheduled_arrival
            OR (OLD.dispatch_status = 'CANCELLED' AND NEW.dispatch_status <> 'CANCELLED');
        IF NOT check_overlap AND NEW.route_id IS NOT DISTINCT FROM OLD.route_id
            AND NEW.route_version_id IS NOT DISTINCT FROM OLD.route_version_id
            AND NEW.dispatch_date IS NOT DISTINCT FROM OLD.dispatch_date THEN
            RETURN NEW;
        END IF;
    END IF;

    -- Lock order matches the application services and protects interval inserts
    -- even when two transactions initially observe an empty assignment set.
    PERFORM 1 FROM arc.fleet_units WHERE id = NEW.fleet_unit_id FOR UPDATE;
    PERFORM 1 FROM arc.drivers WHERE id = NEW.driver_id FOR UPDATE;
    PERFORM 1 FROM arc.routes WHERE id = NEW.route_id FOR UPDATE;

    SELECT * INTO definition FROM arc.route_versions WHERE id = NEW.route_version_id;
    IF definition.id IS NULL OR definition.route_id <> NEW.route_id OR
       NEW.dispatch_date < definition.effective_from OR
       (definition.effective_to IS NOT NULL AND NEW.dispatch_date >= definition.effective_to) THEN
        RAISE EXCEPTION 'ROUTE_VERSION_NOT_EFFECTIVE' USING ERRCODE = '23514';
    END IF;

    check_overlap := TG_OP = 'INSERT';
    IF TG_OP = 'UPDATE' THEN
        check_overlap := NEW.fleet_unit_id IS DISTINCT FROM OLD.fleet_unit_id
            OR NEW.driver_id IS DISTINCT FROM OLD.driver_id
            OR NEW.scheduled_departure IS DISTINCT FROM OLD.scheduled_departure
            OR NEW.scheduled_arrival IS DISTINCT FROM OLD.scheduled_arrival
            OR (OLD.dispatch_status = 'CANCELLED' AND NEW.dispatch_status <> 'CANCELLED');
    END IF;

    IF check_overlap AND (NEW.scheduled_arrival IS NULL OR NEW.scheduled_arrival <= NEW.scheduled_departure) THEN
        RAISE EXCEPTION 'INVALID_ASSIGNMENT_WINDOW' USING ERRCODE = '23514';
    END IF;
    IF check_overlap AND NEW.dispatch_status <> 'CANCELLED' THEN
        IF EXISTS (SELECT 1 FROM arc.dispatch_assignments assignment
            WHERE assignment.fleet_unit_id = NEW.fleet_unit_id AND assignment.id <> NEW.id
              AND assignment.dispatch_status <> 'CANCELLED'
              AND assignment.scheduled_departure < NEW.scheduled_arrival
              AND COALESCE(assignment.scheduled_arrival,
                    CASE WHEN assignment.dispatch_status = 'COMPLETED'
                              AND assignment.actual_arrival > assignment.scheduled_departure
                         THEN assignment.actual_arrival END,
                    'infinity'::TIMESTAMPTZ) > NEW.scheduled_departure) THEN
            RAISE EXCEPTION 'VEHICLE_ASSIGNMENT_OVERLAP' USING ERRCODE = '23514';
        END IF;
        IF EXISTS (SELECT 1 FROM arc.dispatch_assignments assignment
            WHERE assignment.driver_id = NEW.driver_id AND assignment.id <> NEW.id
              AND assignment.dispatch_status <> 'CANCELLED'
              AND assignment.scheduled_departure < NEW.scheduled_arrival
              AND COALESCE(assignment.scheduled_arrival,
                    CASE WHEN assignment.dispatch_status = 'COMPLETED'
                              AND assignment.actual_arrival > assignment.scheduled_departure
                         THEN assignment.actual_arrival END,
                    'infinity'::TIMESTAMPTZ) > NEW.scheduled_departure) THEN
            RAISE EXCEPTION 'DRIVER_ASSIGNMENT_OVERLAP' USING ERRCODE = '23514';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER dispatch_assignment_guard
    BEFORE INSERT OR UPDATE OF fleet_unit_id, driver_id, route_id, route_version_id,
        dispatch_date, scheduled_departure, scheduled_arrival, dispatch_status
    ON arc.dispatch_assignments
    FOR EACH ROW EXECUTE FUNCTION arc.validate_dispatch_assignment();