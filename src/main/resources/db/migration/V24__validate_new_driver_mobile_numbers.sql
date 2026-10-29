-- Do not rewrite existing contact data; validate new entries and changed contact values.
CREATE FUNCTION arc.require_local_driver_mobile() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' OR NEW.contact_number IS DISTINCT FROM OLD.contact_number THEN
        IF NEW.contact_number IS NOT NULL AND NEW.contact_number <> ''
                AND NEW.contact_number !~ '^09[0-9]{9}$' THEN
            RAISE EXCEPTION 'Use an 11-digit mobile number starting with 09'
                USING ERRCODE = '23514', CONSTRAINT = 'ck_driver_local_mobile';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER driver_local_mobile
    BEFORE INSERT OR UPDATE OF contact_number ON arc.drivers
    FOR EACH ROW EXECUTE FUNCTION arc.require_local_driver_mobile();
