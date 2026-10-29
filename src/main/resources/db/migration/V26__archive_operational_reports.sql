ALTER TABLE arc.operational_reports ADD COLUMN archived_at TIMESTAMPTZ;
CREATE INDEX idx_report_archive ON arc.operational_reports (archived_at, reported_at DESC);

-- Keep immutable verification history after guarded deletion, as with dispatch events.
ALTER TABLE arc.report_verification_events DROP CONSTRAINT report_verification_events_report_id_fkey;

CREATE FUNCTION arc.require_existing_report_for_verification() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    PERFORM 1 FROM arc.operational_reports WHERE id = NEW.report_id FOR KEY SHARE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Verification event requires an existing operational report'
            USING ERRCODE = '23503', CONSTRAINT = 'verification_existing_report';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER verification_existing_report
    BEFORE INSERT ON arc.report_verification_events
    FOR EACH ROW EXECUTE FUNCTION arc.require_existing_report_for_verification();

CREATE FUNCTION arc.require_existing_report_for_exception() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.affected_entity_type = 'OPERATIONAL_REPORT' THEN
        PERFORM 1 FROM arc.operational_reports WHERE id = NEW.affected_entity_id FOR KEY SHARE;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Referenced operational report does not exist'
                USING ERRCODE = '23503', CONSTRAINT = 'exception_existing_report';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER exception_existing_report
    BEFORE INSERT OR UPDATE OF affected_entity_type, affected_entity_id ON arc.operational_exceptions
    FOR EACH ROW EXECUTE FUNCTION arc.require_existing_report_for_exception();
