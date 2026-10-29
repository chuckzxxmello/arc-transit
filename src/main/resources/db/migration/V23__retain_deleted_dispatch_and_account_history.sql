-- Retain immutable events and actor-name reservations after guarded archive deletion.
ALTER TABLE arc.dispatch_events DROP CONSTRAINT dispatch_events_assignment_id_fkey;
ALTER TABLE arc.username_reservations DROP CONSTRAINT username_reservations_user_id_fkey;

CREATE FUNCTION arc.require_existing_dispatch_for_event() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    PERFORM 1 FROM arc.dispatch_assignments WHERE id = NEW.assignment_id FOR KEY SHARE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Dispatch event requires an existing assignment'
            USING ERRCODE = '23503', CONSTRAINT = 'dispatch_event_existing_assignment';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER dispatch_event_existing_assignment
    BEFORE INSERT ON arc.dispatch_events
    FOR EACH ROW EXECUTE FUNCTION arc.require_existing_dispatch_for_event();

CREATE FUNCTION arc.require_existing_user_for_reservation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    PERFORM 1 FROM arc.app_users WHERE id = NEW.user_id FOR KEY SHARE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Username reservation requires an existing account'
            USING ERRCODE = '23503', CONSTRAINT = 'username_reservation_existing_account';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER username_reservation_existing_account
    BEFORE INSERT ON arc.username_reservations
    FOR EACH ROW EXECUTE FUNCTION arc.require_existing_user_for_reservation();

-- These three modules use polymorphic IDs. Lock the referenced dispatch against concurrent deletion.
CREATE FUNCTION arc.require_existing_dispatch_reference() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    record_type TEXT;
    record_id BIGINT;
BEGIN
    IF TG_TABLE_NAME = 'incidents' THEN
        record_type := NEW.affected_service_type;
        record_id := NEW.affected_entity_id;
    ELSIF TG_TABLE_NAME = 'operational_reports' THEN
        record_type := NEW.entity_type;
        record_id := NEW.entity_id;
    ELSE
        record_type := NEW.affected_entity_type;
        record_id := NEW.affected_entity_id;
    END IF;
    IF record_type = 'DISPATCH' AND record_id IS NOT NULL THEN
        PERFORM 1 FROM arc.dispatch_assignments WHERE id = record_id FOR KEY SHARE;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Referenced dispatch assignment does not exist'
                USING ERRCODE = '23503', CONSTRAINT = 'operational_existing_dispatch';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER incident_existing_dispatch
    BEFORE INSERT OR UPDATE OF affected_service_type, affected_entity_id ON arc.incidents
    FOR EACH ROW EXECUTE FUNCTION arc.require_existing_dispatch_reference();
CREATE TRIGGER report_existing_dispatch
    BEFORE INSERT OR UPDATE OF entity_type, entity_id ON arc.operational_reports
    FOR EACH ROW EXECUTE FUNCTION arc.require_existing_dispatch_reference();
CREATE TRIGGER exception_existing_dispatch
    BEFORE INSERT OR UPDATE OF affected_entity_type, affected_entity_id ON arc.operational_exceptions
    FOR EACH ROW EXECUTE FUNCTION arc.require_existing_dispatch_reference();
