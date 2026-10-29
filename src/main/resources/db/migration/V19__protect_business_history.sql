CREATE FUNCTION arc.reject_business_history_change() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    -- Hibernate may repeat an unchanged collection foreign key after initial insertion.
    IF TG_OP = 'UPDATE' THEN
        IF NEW IS NOT DISTINCT FROM OLD THEN
            RETURN NEW;
        END IF;
    END IF;
    RAISE EXCEPTION 'Business history records are append-only' USING ERRCODE = '23514';
END;
$$;

CREATE TRIGGER vehicle_availability_history_append_only
    BEFORE UPDATE OR DELETE ON arc.vehicle_availability_history
    FOR EACH ROW EXECUTE FUNCTION arc.reject_business_history_change();
CREATE TRIGGER driver_availability_history_append_only
    BEFORE UPDATE OR DELETE ON arc.driver_availability_history
    FOR EACH ROW EXECUTE FUNCTION arc.reject_business_history_change();
CREATE TRIGGER maintenance_history_append_only
    BEFORE UPDATE OR DELETE ON arc.maintenance_history
    FOR EACH ROW EXECUTE FUNCTION arc.reject_business_history_change();
CREATE TRIGGER dispatch_events_append_only
    BEFORE UPDATE OR DELETE ON arc.dispatch_events
    FOR EACH ROW EXECUTE FUNCTION arc.reject_business_history_change();
CREATE TRIGGER report_verification_events_append_only
    BEFORE UPDATE OR DELETE ON arc.report_verification_events
    FOR EACH ROW EXECUTE FUNCTION arc.reject_business_history_change();
CREATE TRIGGER incident_events_append_only
    BEFORE UPDATE OR DELETE ON arc.incident_events
    FOR EACH ROW EXECUTE FUNCTION arc.reject_business_history_change();
CREATE TRIGGER route_version_stops_immutable
    BEFORE UPDATE OR DELETE ON arc.route_version_stops
    FOR EACH ROW EXECUTE FUNCTION arc.reject_business_history_change();

CREATE FUNCTION arc.guard_historical_route_stop_insert() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    PERFORM 1 FROM arc.routes route JOIN arc.route_versions version ON version.route_id = route.id
        WHERE version.id = NEW.route_version_id FOR UPDATE OF route;
    IF EXISTS (SELECT 1 FROM arc.dispatch_assignments WHERE route_version_id = NEW.route_version_id) THEN
        RAISE EXCEPTION 'Cannot add stops to a route version referenced by trips' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER historical_route_stop_insert_guard
    BEFORE INSERT ON arc.route_version_stops
    FOR EACH ROW EXECUTE FUNCTION arc.guard_historical_route_stop_insert();
