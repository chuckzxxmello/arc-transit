CREATE TABLE arc.username_reservations (
    username VARCHAR(50) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES arc.app_users(id) ON DELETE RESTRICT,
    reserved_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_reserved_username_normalized
        CHECK (username = LOWER(username) AND username = BTRIM(username) AND username <> '')
);

CREATE INDEX idx_username_reservations_user ON arc.username_reservations(user_id);

INSERT INTO arc.username_reservations(username, user_id)
SELECT username, id FROM arc.app_users;

-- Historical actor names must never be reassigned to a different account.
CREATE FUNCTION arc.reserve_account_username() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    owner_id BIGINT;
BEGIN
    INSERT INTO arc.username_reservations(username, user_id)
        VALUES (NEW.username, NEW.id) ON CONFLICT (username) DO NOTHING;
    SELECT user_id INTO owner_id FROM arc.username_reservations WHERE username = NEW.username;
    IF owner_id <> NEW.id THEN
        RAISE EXCEPTION 'Username is already reserved by another account'
            USING ERRCODE = '23505', CONSTRAINT = 'username_reservations_pkey';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER reserve_account_username
    AFTER INSERT OR UPDATE OF username ON arc.app_users
    FOR EACH ROW EXECUTE FUNCTION arc.reserve_account_username();

CREATE TRIGGER username_reservations_append_only
    BEFORE UPDATE OR DELETE ON arc.username_reservations
    FOR EACH ROW EXECUTE FUNCTION arc.reject_business_history_change();
