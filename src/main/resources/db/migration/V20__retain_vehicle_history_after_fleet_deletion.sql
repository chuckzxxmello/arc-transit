-- Availability events retain their historical fleet ID after an unreferenced bus is deleted.
-- Operational foreign keys and the append-only history trigger remain in force.
ALTER TABLE arc.vehicle_availability_history
    DROP CONSTRAINT vehicle_availability_history_fleet_unit_id_fkey;

CREATE FUNCTION arc.require_fleet_unit_for_availability_history() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    PERFORM 1 FROM arc.fleet_units WHERE id = NEW.fleet_unit_id FOR KEY SHARE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Availability history requires an existing fleet unit'
            USING ERRCODE = '23503', CONSTRAINT = 'vehicle_history_existing_fleet_unit';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER vehicle_history_existing_fleet_unit
    BEFORE INSERT ON arc.vehicle_availability_history
    FOR EACH ROW EXECUTE FUNCTION arc.require_fleet_unit_for_availability_history();
