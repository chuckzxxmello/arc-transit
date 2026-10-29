ALTER TABLE arc.maintenance_records ADD COLUMN archived_at TIMESTAMPTZ;

UPDATE arc.maintenance_records
SET archived_at = COALESCE(completed_at, updated_at, created_at)
WHERE maintenance_status = 'COMPLETED' AND archived_at IS NULL;

UPDATE arc.incidents
SET archived_at = COALESCE(updated_at, resolved_at, reported_at)
WHERE status = 'CLOSED' AND archived_at IS NULL;

CREATE INDEX idx_maintenance_archive ON arc.maintenance_records (archived_at, created_at DESC);
CREATE INDEX idx_incident_archive ON arc.incidents (archived_at, reported_at DESC);
