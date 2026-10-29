ALTER TABLE arc.operational_exceptions ADD COLUMN rule_code VARCHAR(80);

UPDATE arc.operational_exceptions SET rule_code = CASE
    WHEN exception_type = 'CONTRADICTION' AND affected_entity_type = 'FLEET'
        THEN 'MAINTENANCE_AVAILABILITY_CONFLICT'
    WHEN exception_type = 'STALE_STATE' AND affected_entity_type = 'DISPATCH'
        THEN 'AGING_ACTIVE_TRIP'
    WHEN exception_type = 'STALE_STATE' AND affected_entity_type = 'INCIDENT'
        THEN 'AGING_OPEN_INCIDENT'
    ELSE exception_type || '_' || affected_entity_type
END;

ALTER TABLE arc.operational_exceptions ALTER COLUMN rule_code SET NOT NULL;
DROP INDEX arc.ux_exception_open_entity;
CREATE UNIQUE INDEX ux_exception_open_rule_entity
    ON arc.operational_exceptions (rule_code, affected_entity_type, affected_entity_id)
    WHERE exception_status IN ('OPEN', 'ACKNOWLEDGED', 'INVESTIGATING');

ALTER TABLE arc.audit_logs ADD COLUMN outcome VARCHAR(20) NOT NULL DEFAULT 'SUCCESS';
UPDATE arc.audit_logs SET outcome = 'FAILURE'
    WHERE action_name IN ('USER_LOGIN_FAILED', 'RECONCILIATION_FAILED');
ALTER TABLE arc.audit_logs ADD CONSTRAINT ck_audit_outcome
    CHECK (outcome IN ('SUCCESS', 'FAILURE', 'DENIED'));

CREATE FUNCTION arc.reject_audit_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Audit records are append-only';
END;
$$;
CREATE TRIGGER audit_logs_append_only
    BEFORE UPDATE OR DELETE ON arc.audit_logs
    FOR EACH ROW EXECUTE FUNCTION arc.reject_audit_change();
