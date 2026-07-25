/*
 * Tables:
 *      roles
 *      app_users
 *      user_roles
 *
 * Important rules:
 * - SYSTEM_ADMIN and OPERATIONS_STAFF roles only.
 * - Flyway inserts the two fixed role definitions when this 
 *    migration is first applied to a new database.
 *
 * - The passwords are stored only as BCrypt hashes.
 * - Drivers are operational records and do not automatically receive accounts.
 * - Java/JPA manages the application timestamps.
 * - User data records are archived (soft-delete) instead of hard-deleted.
 */

CREATE TABLE arc.roles (

    role_code VARCHAR(40) NOT NULL,
    role_name VARCHAR(80) NOT NULL,
    description VARCHAR(255),

    CONSTRAINT pk_roles
        PRIMARY KEY (role_code),

    /*
     * Prevents a role from having an empty display name.
     */
    CONSTRAINT ck_roles_role_name_not_blank
        CHECK (BTRIM(role_name) <> ''),

    /*
     * A role_code is either SYSTEM_ADMIN or OPERATIONS_STAFF.
     */
    CONSTRAINT ck_roles_role_code
        CHECK (
            role_code IN (
                'SYSTEM_ADMIN',
                'OPERATIONS_STAFF'
            )
        )
);


CREATE TABLE arc.app_users (

    id BIGINT GENERATED ALWAYS AS IDENTITY,

    /*
     * Lowercase login name.
     *
     * Ex:
     *      system.admin
     *      operations.staff
     */
    username VARCHAR(50) NOT NULL,

    /*
     * BCrypt password hash.
     */
    password_hash VARCHAR(100) NOT NULL,

    /*
     * Staff name displayed in the application.
     */
    display_name VARCHAR(120) NOT NULL,

    /*
     * Optional contact email.
     */
    email VARCHAR(254),

    /*
     * Current authentication state.
     *
     * ACTIVE:
     * The user may authenticate.
     *
     * DISABLED:
     * Administratively prevented from authenticating.
     *
     * LOCKED:
     * Manually locked by an administrator.
     */
    account_status VARCHAR(20) NOT NULL,

    /*
     * Most recent successful authentication timestamp.
     * if last_login_at is NULL, then the user has never successfully logged in.
     */
    last_login_at TIMESTAMPTZ,

    /*
    * Time when the app_user record was first  (e.g. date created).
    *
    * The application will supply this value through JPA auditing
    * when the entity is first saved. PostgreSQL stores it as
    * TIMESTAMPTZ.
    *
    * A direct SQL insert must provide this value manually because
    * the database does not define a timestamp default.
    */
    created_at TIMESTAMPTZ NOT NULL,

    /*
    * Time when the app_user record was last changed (e.g. date modified).
    *
    * JPA auditing will assign this value when the entity is first
    * saved and replace it after later updates.
    *
    * PostgreSQL does not update this column automatically because
    * the database has no update trigger for it.
    */
    updated_at TIMESTAMPTZ NOT NULL,

    /*
     * Soft-deactivation timestamp.
     *
     * Archived accounts remain stored for authorization
     * and audit-history references.
     */
    archived_at TIMESTAMPTZ,

    /*
     * JPA optimistic-lock version.
     */
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_app_users
        PRIMARY KEY (id),

    /*
     * Usernames remain permanently reserved, including after archival.
     */
    CONSTRAINT ux_app_users_username
        UNIQUE (username),

    /*
     * Usernames must be lowercase, trimmed, and nonblank.
     *
     * The Java authentication and account-creation service should
     * normalize input before querying or saving it.
     * The constraint rejects inconsistent stored values.
     *
     * Ex:
     *      accepted -> "operations.staff"
     *      rejected -> "Operations.Staff"
     */
    CONSTRAINT ck_app_users_username_lowercase
        CHECK (username = LOWER(username)),

    /*
     * Prevents a missing or empty username.
     */
    CONSTRAINT ck_app_users_username_not_blank
        CHECK (BTRIM(username) <> ''),

    CONSTRAINT ck_app_users_display_name_not_blank
        CHECK (BTRIM(display_name) <> ''),

    /*
     * One-way encoded password produced by Spring Security's
     * PasswordEncoder.
     *
     * PostgreSQL stores the encoded value but does not authenticate
     * the user or compare passwords.
     */
    CONSTRAINT ck_app_users_password_hash_not_blank
        CHECK (BTRIM(password_hash) <> ''),


    /*
     * An optional email must be lowercase, trimmed, and nonblank.
     * NULL means that no email address was supplied.
     */
    CONSTRAINT ck_app_users_email_normalized
        CHECK (
            email IS NULL
            OR email = BTRIM(LOWER(email))
    ),
    CONSTRAINT ck_app_users_email_not_blank
        CHECK (
            email IS NULL
            OR BTRIM(email) <> ''
    ),

    /*
     * If the account states is unknown, reject it.
     */
    CONSTRAINT ck_app_users_account_status
        CHECK (
            account_status IN (
                'ACTIVE',
                'DISABLED',
                'LOCKED'
            )
        )
);

/*
 * Implements a case-insensitive email uniqueness if email exists.
 *
 * Ex:
 *      staff@arctransit.local
 *      STAFF@ARCTRANSIT.LOCAL
 *
 * PostgreSQL treats these as the same email for uniqueness purposes.
 * Multiple NULL values remain allowed because email is OPTIONAL.
 */
CREATE UNIQUE INDEX ux_app_users_email_ci
    ON arc.app_users (LOWER(email))
    WHERE email IS NOT NULL
        AND archived_at IS NULL;


CREATE TABLE arc.user_roles (

    /*
     * Staff account receiving the role.
     */
    user_id BIGINT NOT NULL,

    /*
     * Seeded role assigned to the account.
     */
    role_code VARCHAR(40) NOT NULL,

    /*
     * Administrator who granted the role.
     *
     * NULL is permitted for controlled system setup,
     * including the first development administrator.
     */
    assigned_by_user_id BIGINT,

    /*
     * Time when the role was assigned.
     * The application supplies this value when creating an assignment.
     * A direct SQL insert must provide it manually.
     */
    assigned_at TIMESTAMPTZ NOT NULL,

    /*
     * The combination of user_id and role_code identifies one
     * role assignment.
     *
     * A user may hold different roles, but the same role cannot
     * be assigned to that user more than once.
     */
    CONSTRAINT pk_user_roles
        PRIMARY KEY (user_id, role_code),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES arc.app_users (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_code)
        REFERENCES arc.roles (role_code)
        ON DELETE RESTRICT,

    /*
     * Records which administrator granted the role.
     *
     * The value may be NULL only for controlled system bootstrap.
     * Once an administrator is referenced here, PostgreSQL prevents
     * that account from being physically deleted.
     *
     * Normal account removal uses archived_at instead.
     */
    CONSTRAINT fk_user_roles_assigned_by
        FOREIGN KEY (assigned_by_user_id)
        REFERENCES arc.app_users (id)
        ON DELETE RESTRICT
);


/*
 * Administrators assign these roles to accounts but do not
 * create new role definitions through the user interface.
 */
INSERT INTO arc.roles (
    role_code,
    role_name,
    description
)
VALUES
    (
        'SYSTEM_ADMIN',
        'System Administrator',
        'Manages staff accounts, role assignments, configuration, audit records, and all operational modules.'
    ),
    (
        'OPERATIONS_STAFF',
        'Operations Staff',
        'Handles daily fleet, route, dispatch, incident, maintenance, status, and dashboard operations.'
    );