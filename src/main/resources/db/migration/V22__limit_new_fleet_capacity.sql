-- Preserve legacy capacities while enforcing the new limit on inserts and capacity edits.
CREATE FUNCTION arc.require_supported_fleet_capacity() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' OR NEW.capacity IS DISTINCT FROM OLD.capacity THEN
        IF NEW.capacity < 1 OR NEW.capacity > 80 THEN
            RAISE EXCEPTION 'Bus capacity must be between 1 and 80'
                USING ERRCODE = '23514', CONSTRAINT = 'ck_fleet_capacity_range';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER fleet_capacity_range
    BEFORE INSERT OR UPDATE OF capacity ON arc.fleet_units
    FOR EACH ROW EXECUTE FUNCTION arc.require_supported_fleet_capacity();
