ALTER TABLE arc.fleet_units
    ADD COLUMN administrative_status VARCHAR(30),
    ADD COLUMN availability VARCHAR(30),
    ADD COLUMN availability_effective_at TIMESTAMPTZ;

UPDATE arc.fleet_units
SET administrative_status = CASE WHEN operational_status = 'INACTIVE' THEN 'INACTIVE' ELSE 'ACTIVE' END,
    availability = CASE operational_status
        WHEN 'ACTIVE' THEN 'AVAILABLE'
        WHEN 'UNDER_MAINTENANCE' THEN 'MAINTENANCE'
        WHEN 'OUT_OF_SERVICE' THEN 'OUT_OF_SERVICE'
        ELSE 'UNKNOWN' END,
    availability_effective_at = updated_at;

ALTER TABLE arc.fleet_units
    ALTER COLUMN administrative_status SET NOT NULL,
    ALTER COLUMN availability SET NOT NULL,
    ALTER COLUMN availability_effective_at SET NOT NULL,
    ADD CONSTRAINT ck_fleet_administrative_status CHECK (administrative_status IN ('ACTIVE', 'INACTIVE')),
    ADD CONSTRAINT ck_fleet_availability CHECK (availability IN ('AVAILABLE', 'MAINTENANCE', 'INSPECTION_HOLD', 'OUT_OF_SERVICE', 'UNKNOWN'));

ALTER TABLE arc.drivers
    ADD COLUMN availability VARCHAR(30),
    ADD COLUMN availability_effective_at TIMESTAMPTZ;

-- Preserve the legacy application's recorded eligibility; future records start UNKNOWN.
UPDATE arc.drivers
SET availability = CASE WHEN employment_status = 'ACTIVE' THEN 'AVAILABLE' ELSE 'UNAVAILABLE' END,
    availability_effective_at = updated_at;

ALTER TABLE arc.drivers
    ALTER COLUMN availability SET NOT NULL,
    ALTER COLUMN availability_effective_at SET NOT NULL,
    ADD CONSTRAINT ck_driver_availability CHECK (availability IN ('AVAILABLE', 'UNAVAILABLE', 'UNKNOWN'));

CREATE TABLE arc.vehicle_availability_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fleet_unit_id BIGINT NOT NULL REFERENCES arc.fleet_units(id),
    previous_availability VARCHAR(30),
    availability VARCHAR(30) NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    recorded_by VARCHAR(100) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    verification_status VARCHAR(30) NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    CONSTRAINT ck_vehicle_history_availability CHECK (availability IN ('AVAILABLE', 'MAINTENANCE', 'INSPECTION_HOLD', 'OUT_OF_SERVICE', 'UNKNOWN')),
    CONSTRAINT ck_vehicle_history_previous CHECK (previous_availability IS NULL OR previous_availability IN ('AVAILABLE', 'MAINTENANCE', 'INSPECTION_HOLD', 'OUT_OF_SERVICE', 'UNKNOWN')),
    CONSTRAINT ck_vehicle_history_context CHECK (BTRIM(recorded_by) <> '' AND BTRIM(reason) <> '' AND effective_at <= recorded_at),
    CONSTRAINT ck_vehicle_history_verification CHECK (verification_status = 'UNVERIFIED')
);

CREATE INDEX idx_vehicle_availability_history
    ON arc.vehicle_availability_history (fleet_unit_id, recorded_at DESC, id DESC);

CREATE TABLE arc.driver_availability_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    driver_id BIGINT NOT NULL REFERENCES arc.drivers(id),
    previous_availability VARCHAR(30),
    availability VARCHAR(30) NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    recorded_by VARCHAR(100) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    verification_status VARCHAR(30) NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    CONSTRAINT ck_driver_history_availability CHECK (availability IN ('AVAILABLE', 'UNAVAILABLE', 'UNKNOWN')),
    CONSTRAINT ck_driver_history_previous CHECK (previous_availability IS NULL OR previous_availability IN ('AVAILABLE', 'UNAVAILABLE', 'UNKNOWN')),
    CONSTRAINT ck_driver_history_context CHECK (BTRIM(recorded_by) <> '' AND BTRIM(reason) <> '' AND effective_at <= recorded_at),
    CONSTRAINT ck_driver_history_verification CHECK (verification_status = 'UNVERIFIED')
);

CREATE INDEX idx_driver_availability_history
    ON arc.driver_availability_history (driver_id, recorded_at DESC, id DESC);

-- These snapshots do not reconstruct transitions that the legacy system never retained.
INSERT INTO arc.vehicle_availability_history
    (fleet_unit_id, availability, effective_at, recorded_at, recorded_by, source_type, verification_status, reason)
SELECT id, availability, availability_effective_at, GREATEST(CURRENT_TIMESTAMP, availability_effective_at),
    'SYSTEM', 'LEGACY_MIGRATION', 'UNVERIFIED', 'Current availability migrated from legacy operational status.'
FROM arc.fleet_units;

INSERT INTO arc.driver_availability_history
    (driver_id, availability, effective_at, recorded_at, recorded_by, source_type, verification_status, reason)
SELECT id, availability, availability_effective_at, GREATEST(CURRENT_TIMESTAMP, availability_effective_at),
    'SYSTEM', 'LEGACY_MIGRATION', 'UNVERIFIED', 'Current availability migrated from legacy employment eligibility.'
FROM arc.drivers;