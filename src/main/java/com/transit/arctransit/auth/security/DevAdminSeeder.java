package com.transit.arctransit.auth.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Creates the initial development administrator when the dev profile is active
 * and no administrator account exists.
 *
 * The initial password is read from external configuration, encoded before
 * persistence, and never printed to application logs.
 */

/*
 * The previous implementation expects Flyway to insert an administrator
 * account before application startup. That migration has been removed.
 *
 * Replace this class later with a development-profile bootstrap that
 * creates the administrator only when absent and reads its initial
 * password from an environment variable.
 */

@Component
@Profile("dev")
public class DevAdminSeeder implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String initialPassword;

    private static final Logger LOGGER = LoggerFactory.getLogger(DevAdminSeeder.class);

    private static final String DEV_ADMIN_USERNAME = "admin";
    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";

    public DevAdminSeeder(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${ARC_DEV_ADMIN_PASSWORD:}") String initialPassword) {

        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.initialPassword = initialPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        Integer existingAdminCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM arc.app_users
                        WHERE username = ?
                        """,
                Integer.class,
                DEV_ADMIN_USERNAME);

        if (existingAdminCount != null && existingAdminCount > 0) {
            return;
        }

        if (initialPassword.isBlank()) {
            throw new IllegalStateException(
                    "ARC_DEV_ADMIN_PASSWORD must be provided when the dev "
                            + "profile creates the initial administrator.");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        Long administratorId = jdbcTemplate.queryForObject(
                """
                        INSERT INTO arc.app_users (
                            username,
                            password_hash,
                            display_name,
                            account_status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, 'ACTIVE', ?, ?)
                        RETURNING id
                        """,
                Long.class,
                DEV_ADMIN_USERNAME,
                passwordEncoder.encode(initialPassword),
                "Development Administrator",
                now,
                now);

        jdbcTemplate.update(
                """
                        INSERT INTO arc.user_roles (
                            user_id,
                            role_code,
                            assigned_by_user_id,
                            assigned_at
                        )
                        VALUES (?, ?, NULL, ?)
                        """,
                administratorId,
                SYSTEM_ADMIN_ROLE,
                now);

        LOGGER.info(
                "Created development administrator account '{}'.",
                DEV_ADMIN_USERNAME);
    }
}
