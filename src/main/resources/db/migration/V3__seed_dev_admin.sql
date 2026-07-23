/*
 * Seeds a development administrator account for local testing.
 *
 * Username: admin
 * Password: admin (set at runtime by DevAdminSeeder.java)
 *
 * This migration inserts the row with a temporary placeholder hash.
 * The DevAdminSeeder ApplicationRunner replaces it with a real
 * BCrypt hash on the first application startup.
 *
 * This migration must not be included in production deployments.
 */

INSERT INTO arc.app_users (
    username,
    password_hash,
    display_name,
    email,
    account_status,
    created_at,
    updated_at
)
VALUES (
    'admin',
    'PLACEHOLDER_REPLACED_BY_APP',
    'System Administrator',
    'admin@arctransit.local',
    'ACTIVE',
    NOW(),
    NOW()
);

INSERT INTO arc.user_roles (
    user_id,
    role_code,
    assigned_at
)
VALUES (
    (SELECT id FROM arc.app_users WHERE username = 'admin'),
    'SYSTEM_ADMIN',
    NOW()
);
