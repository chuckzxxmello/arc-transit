package com.transit.arctransit.auth.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Replaces the placeholder password hash for the seeded dev admin
 * account with a real BCrypt hash on the first application startup.
 *
 * This class is only needed for development and should not be
 * included in production deployments.
 */
@Component
public class DevAdminSeeder implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public DevAdminSeeder(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {

        String currentHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM arc.app_users WHERE username = 'admin'",
                String.class);

        /*
         * Only update if the hash is still the Flyway placeholder
         * or does not look like a valid BCrypt hash.
         */
        if (currentHash == null || !currentHash.startsWith("$2")) {
            String bcryptHash = passwordEncoder.encode("admin");

            jdbcTemplate.update(
                    "UPDATE arc.app_users SET password_hash = ? WHERE username = 'admin'",
                    bcryptHash);

            System.out.println("[DevAdminSeeder] Dev admin password set. Login with admin / admin");
        }
    }
}
